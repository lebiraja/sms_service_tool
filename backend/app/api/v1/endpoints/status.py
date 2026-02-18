from fastapi import APIRouter, Depends
from datetime import datetime
from typing import Optional
from pydantic import BaseModel

from app.services.ws_manager import WebSocketManager

router = APIRouter(tags=["status"])


class DeviceStatusResponse(BaseModel):
    """Device connection status."""
    connected: bool
    device_id: Optional[str] = None
    device_name: Optional[str] = None
    android_version: Optional[str] = None
    app_version: Optional[str] = None
    sim_operator: Optional[str] = None
    sim_country: Optional[str] = None
    connected_at: Optional[datetime] = None
    last_ping_at: Optional[datetime] = None


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    version: str
    device_connected: bool


async def get_ws_manager() -> WebSocketManager:
    """Dependency: get the global WebSocket manager."""
    # This will be overridden with the actual manager in main.py
    raise NotImplementedError("WebSocket manager not initialized")


@router.get("/device/status", response_model=DeviceStatusResponse)
async def device_status(
    ws_manager: WebSocketManager = Depends(get_ws_manager),
):
    """Get current device connection state."""
    device_info = ws_manager.get_device_info()
    if not device_info:
        return DeviceStatusResponse(connected=False)

    return DeviceStatusResponse(
        connected=True,
        device_id=device_info.get("device_id"),
        device_name=device_info.get("device_name"),
        android_version=device_info.get("android_version"),
        app_version=device_info.get("app_version"),
        sim_operator=device_info.get("sim_operator"),
        sim_country=device_info.get("sim_country"),
        connected_at=device_info.get("connected_at"),
        last_ping_at=device_info.get("last_ping_at"),
    )


@router.get("/health", response_model=HealthResponse)
async def health(ws_manager: WebSocketManager = Depends(get_ws_manager)):
    """Health check endpoint. Used by Android app to verify backend is online."""
    return HealthResponse(
        status="ok",
        version="1.0.0",
        device_connected=ws_manager.is_connected(),
    )
