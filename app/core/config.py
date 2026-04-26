import logging
from pydantic_settings import BaseSettings, SettingsConfigDict

# Logging Configuration
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("app.log")
    ]
)
logger = logging.getLogger("ReTailX")

class Settings(BaseSettings):
    TWILIO_ACCOUNT_SID: str
    TWILIO_AUTH_TOKEN: str
    TWILIO_MESSAGING_SERVICE_SID: str
    PIYUSH_NUMBER: str
    RAJAT_NUMBER: str

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

settings = Settings()
