from app.core.entities.message import Message
from app.core.use_cases.interfaces import NotificationGateway

class SendNotificationUseCase:
    def __init__(self, gateway: NotificationGateway):
        self.gateway = gateway

    def execute(self, to_name: str, body: str) -> str:
        message = Message(to_name=to_name, body=body)
        return self.gateway.send(message)
