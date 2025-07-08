import os
from fastapi import FastAPI, UploadFile, File, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from sqlalchemy import create_engine, Column, Integer, String, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

from FaceTask import recognize_face_task, compare_faces_task

# --- Database Setup ---
DATABASE_URL = "sqlite:///./facebytes.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(bind=engine)
Base = declarative_base()

class TaskResult(Base):
    __tablename__ = "task_results"
    id = Column(Integer, primary_key=True, index=True)
    task_id = Column(String, unique=True, index=True)
    status = Column(String)
    result = Column(Text)

Base.metadata.create_all(bind=engine)


# --- FastAPI Setup ---
api_server = FastAPI(title="DeepFace Recognition API")

class RecognitionRequest(BaseModel):
    db_path: str

class CompareRequest(BaseModel):
    file1: UploadFile
    file2: UploadFile

# --- API Endpoints ---

@api_server.post("/recognize/")
async def recognize_face(background_tasks: BackgroundTasks, file: UploadFile = File(...), request: RecognitionRequest = None):
    if not request or not request.db_path:
        raise HTTPException(status_code=400, detail="Missing database path")
    # Save uploaded image
    image_path = f"temp_{file.filename}"
    if not file.filename.lower().endswith(('.png', '.jpg', '.jpeg')):
        raise HTTPException(status_code=400, detail="Invalid file type. Only PNG, JPG, and JPEG are allowed.")
    # Write the uploaded file to a temporary location
    with open(image_path, "wb") as buffer:
        buffer.write(await file.read())
    # Launch Celery task
    task = recognize_face_task.delay(image_path, request.db_path)
    # Save task to DB
    db = SessionLocal()
    db.add(TaskResult(task_id=task.id, status="PENDING", result=""))
    db.commit()
    db.close()
    # Remove temp image asynchronously
    background_tasks.add_task(os.remove, image_path)
    return {"task_id": task.id}


@api_server.post("/recognize/batch/")
async def recognize_face_batch(background_tasks: BackgroundTasks, files: list[UploadFile] = File(...), request: RecognitionRequest = None):
    if not request or not request.db_path:
        raise HTTPException(status_code=400, detail="Missing database path")
    if not files:
        raise HTTPException(status_code=400, detail="No files uploaded")

    task_ids = []
    for file in files:
        if not file.filename.lower().endswith(('.png', '.jpg', '.jpeg')):
            raise HTTPException(status_code=400, detail=f"Invalid file type for {file.filename}. Only PNG, JPG, and JPEG are allowed.")
        # Save uploaded image
        image_path = f"temp_{file.filename}"
        with open(image_path, "wb") as buffer:
            buffer.write(await file.read())
        # Launch Celery task
        task = recognize_face_task.delay(image_path, request.db_path)
        task_ids.append(task.id)
        # Save task to DB
        db = SessionLocal()
        db.add(TaskResult(task_id=task.id, status="PENDING", result=""))
        db.commit()
        db.close()
        # Remove temp image asynchronously
        background_tasks.add_task(os.remove, image_path)

    return {"task_ids": task_ids}


@api_server.post("/compare")
async def compare_faces(background_tasks: BackgroundTasks, file1: UploadFile = File(...), file2: UploadFile = File(...)):
    if not file1 or not file2:
        raise HTTPException(status_code=400, detail="Both files must be provided")
    if not (file1.filename.lower().endswith(('.png', '.jpg', '.jpeg')) and file2.filename.lower().endswith(('.png', '.jpg', '.jpeg'))):
        raise HTTPException(status_code=400, detail="Invalid file type. Only PNG, JPG, and JPEG are allowed.")

    # Save uploaded images
    image_path1 = f"temp_{file1.filename}"
    image_path2 = f"temp_{file2.filename}"
    with open(image_path1, "wb") as buffer:
        buffer.write(await file1.read())
    with open(image_path2, "wb") as buffer:
        buffer.write(await file2.read())

    # Launch Celery task
    task = compare_faces_task.delay(image_path1, image_path2)

    # Remove temp images asynchronously
    background_tasks.add_task(os.remove, image_path1)
    background_tasks.add_task(os.remove, image_path2)

    return {"task_id": task.id}


@api_server.post("/compare/batch")
async def compare_faces_batch(background_tasks: BackgroundTasks, files: list[UploadFile] = File(...)):
    if len(files) < 2:
        raise HTTPException(status_code=400, detail="At least two files must be provided")
    if not all(file.filename.lower().endswith(('.png', '.jpg', '.jpeg')) for file in files):
        raise HTTPException(status_code=400, detail="Invalid file type. Only PNG, JPG, and JPEG are allowed.")

    task_ids = []
    for i in range(len(files) - 1):
        image_path1 = f"temp_{files[i].filename}"
        image_path2 = f"temp_{files[i + 1].filename}"
        with open(image_path1, "wb") as buffer:
            buffer.write(await files[i].read())
        with open(image_path2, "wb") as buffer:
            buffer.write(await files[i + 1].read())

        # Launch Celery task
        task = compare_faces_task.delay(image_path1, image_path2)
        task_ids.append(task.id)

        # Remove temp images asynchronously
        background_tasks.add_task(os.remove, image_path1)
        background_tasks.add_task(os.remove, image_path2)

    return {"task_ids": task_ids}


@api_server.get("/result/{task_id}")
def get_result(task_id: str):
    db = SessionLocal()
    task = db.query(TaskResult).filter(TaskResult.task_id == task_id).first()
    db.close()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    # Check Celery task status
    celery_task = celery.AsyncResult(task_id)
    if celery_task.state == "SUCCESS":
        task.status = "SUCCESS"
        task.result = celery_task.result
    elif celery_task.state == "FAILURE":
        task.status = "FAILURE"
        task.result = str(celery_task.result)
    else:
        task.status = celery_task.state
    return {"status": task.status, "result": task.result}

@api_server.get("/")
def read_root():
    return {"message": "FaceBytes: Face Recognition API"}


def main():
    import uvicorn
    uvicorn.run(api_server, host="localhost", port=8000)


if __name__ == "__main__":
    exit(main())