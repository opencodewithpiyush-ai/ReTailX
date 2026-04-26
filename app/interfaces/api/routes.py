from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from starlette.concurrency import run_in_threadpool
from app.core.use_cases.send_notification import SendNotificationUseCase
from app.infrastructure.twilio_gateway import TwilioGateway

router = APIRouter()

class SendMessageRequest(BaseModel):
    receiver: str # 'Piyush' or 'Rajat'
    message: str

# Singleton pattern for production
_gateway = TwilioGateway()

def get_send_notification_use_case():
    return SendNotificationUseCase(_gateway)

@router.post("/send-sms")
async def send_sms(
    request: SendMessageRequest,
    use_case: SendNotificationUseCase = Depends(get_send_notification_use_case)
):
    try:
        # Blocking call ko threadpool mein run kar rahe hain
        sid = await run_in_threadpool(
            use_case.execute, 
            to_name=request.receiver, 
            body=request.message
        )
        return {"status": "success", "message_sid": sid}
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))
