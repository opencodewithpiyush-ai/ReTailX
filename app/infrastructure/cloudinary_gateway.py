import cloudinary
import cloudinary.uploader
import os
from dotenv import load_dotenv

load_dotenv()

class CloudinaryGateway:
    def __init__(self):
        cloudinary.config(
            cloud_name=os.getenv("CLOUDINARY_CLOUD_NAME"),
            api_key=os.getenv("CLOUDINARY_API_KEY"),
            api_secret=os.getenv("CLOUDINARY_API_SECRET"),
            secure=True
        )

    def upload_image(self, file_bytes, public_id=None):
        try:
            result = cloudinary.uploader.upload(file_bytes, public_id=public_id)
            return {
                "url": result.get("secure_url"),
                "public_id": result.get("public_id")
            }
        except Exception as e:
            raise Exception(f"Cloudinary upload failed: {str(e)}")

    def delete_image(self, public_id):
        try:
            result = cloudinary.uploader.destroy(public_id)
            return result.get("result") == "ok"
        except Exception as e:
            raise Exception(f"Cloudinary deletion failed: {str(e)}")
