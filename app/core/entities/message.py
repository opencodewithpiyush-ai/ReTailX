from pydantic import BaseModel
from typing import Optional

class Message(BaseModel):
    body: str
    to_name: str  # 'Piyush' or 'Rajat'
    to_number: Optional[str] = None
