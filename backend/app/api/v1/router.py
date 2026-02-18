from fastapi import APIRouter

from app.api.v1.endpoints import sms, status, device

router = APIRouter()

# Include SMS endpoints
router.include_router(sms.router)

# Include status endpoints
router.include_router(status.router)

# Include device endpoints (HTTP polling)
router.include_router(device.router)
