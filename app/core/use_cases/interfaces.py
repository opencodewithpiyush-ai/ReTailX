from abc import ABC, abstractmethod
from app.core.entities.message import Message

class NotificationGateway(ABC):
    @abstractmethod
    def send(self, message: Message) -> str:
        pass
