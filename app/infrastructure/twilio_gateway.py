from twilio.rest import Client
from app.core.entities.message import Message
from app.core.use_cases.interfaces import NotificationGateway
from app.core.config import settings, logger

class TwilioGateway(NotificationGateway):
    def __init__(self):
        try:
            self.client = Client(settings.TWILIO_ACCOUNT_SID, settings.TWILIO_AUTH_TOKEN)
            self.messaging_service_sid = settings.TWILIO_MESSAGING_SERVICE_SID
            
            self.contacts = {
                "Piyush": settings.PIYUSH_NUMBER,
                "Rajat": settings.RAJAT_NUMBER
            }
            logger.info("TwilioGateway initialized successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize TwilioGateway: {str(e)}")
            raise

    def send(self, message: Message) -> str:
        target_number = message.to_number or self.contacts.get(message.to_name)
        
        if not target_number:
            logger.error(f"Target number not found for: {message.to_name}")
            raise ValueError(f"No number found for contact: {message.to_name}")

        try:
            logger.info(f"Sending SMS to {message.to_name} ({target_number})")
            response = self.client.messages.create(
                messaging_service_sid=self.messaging_service_sid,
                body=message.body,
                to=target_number
            )
            logger.info(f"SMS sent successfully. SID: {response.sid}")
            return response.sid
        except Exception as e:
            logger.error(f"Twilio failed to send message: {str(e)}")
            raise
