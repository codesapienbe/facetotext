from deepface import DeepFace
from celery import Celery
import os

# --- Celery Setup ---
# In memory broken:  celery = Celery(__name__, broker="memory://", backend="cache+memory://")
celery_app = Celery(__name__, broker="redis://localhost:6379/0", backend="db+sqlite:///./facebytes.db")

@celery_app.task
def recognize_face_task(image_path: str, db_path: str):
    try:
        result = DeepFace.find(img_path=image_path, db_path=db_path, enforce_detection=False)
        return result.to_json()
    except Exception as e:
        return str(e)

@celery_app.task
def compare_faces_task(image_path1: str, image_path2: str):
    try:
        result = DeepFace.verify(img1_path=image_path1, img2_path=image_path2, enforce_detection=False)
        return result
    except Exception as e:
        return str(e)