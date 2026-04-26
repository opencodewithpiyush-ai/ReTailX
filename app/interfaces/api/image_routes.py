from fastapi import APIRouter, UploadFile, File, HTTPException
from app.infrastructure.cloudinary_gateway import CloudinaryGateway

router = APIRouter()
cloudinary_gateway = CloudinaryGateway()

@router.post("/upload")
async def upload_image(file: UploadFile = File(...)):
    try:
        file_bytes = await file.read()
        result = cloudinary_gateway.upload_image(file_bytes)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.delete("/delete/{public_id}")
async def delete_image(public_id: str):
    try:
        success = cloudinary_gateway.delete_image(public_id)
        if success:
            return {"message": "Image deleted successfully"}
        else:
            raise HTTPException(status_code=404, detail="Image not found or could not be deleted")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
