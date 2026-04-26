import sys
import os

# Project root ko path mein add kar rahe hain taaki 'app' module mil sake
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI
from app.interfaces.api.routes import router

app = FastAPI(title="ReTailX Messaging API")

app.include_router(router, prefix="/api/v1")

@app.get("/")
async def root():
    return {"message": "Welcome to ReTailX Messaging API"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
