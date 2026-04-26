import sys
import os

# Add the project root to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from app.core.entities.message import Message
from app.infrastructure.twilio_gateway import TwilioGateway

def test_config():
    gateway = TwilioGateway()
    print("Contacts in config:")
    for name, number in gateway.contacts.items():
        print(f" - {name}: {number}")
    
    msg = Message(to_name="Piyush", body="Test")
    print(f"Test message created for {msg.to_name}")

if __name__ == "__main__":
    test_config()
