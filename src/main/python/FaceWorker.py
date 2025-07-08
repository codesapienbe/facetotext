def main():
    # Start celery worker using subprocess
    import subprocess
    import sys
    import os

    celery_worker = subprocess.Popen(
        [sys.executable, "-m", "celery", "-A", "FaceApi.celery_app", "worker", "--loglevel=info"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )

    try:
        # Wait for the worker to start
        stdout, stderr = celery_worker.communicate(timeout=10)
        if stdout:
            print("Celery Worker Output:", stdout)
        if stderr:
            print("Celery Worker Errors:", stderr)

        if celery_worker.returncode != 0:
            print("Celery Worker failed to start with return code:", celery_worker.returncode)
            sys.exit(1)

    except subprocess.TimeoutExpired:
        print("Celery Worker did not start in time, terminating...")
        celery_worker.terminate()
        sys.exit(1)

    except KeyboardInterrupt:
        print("KeyboardInterrupt received, terminating Celery Worker...")
        celery_worker.terminate()
        sys.exit(0)

    except Exception as e:
        print("An error occurred:", str(e))
        celery_worker.terminate()
        sys.exit(1)



if __name__ == "__main__":
    main()