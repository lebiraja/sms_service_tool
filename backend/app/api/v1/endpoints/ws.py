import logging
import json
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from datetime import datetime

from app.services.ws_manager import WebSocketManager
from app.models.message import (
    MessageType,
    DeviceInfoMessage,
    StatusUpdateMessage,
    PongMessage,
    ErrorMessage,
)

logger = logging.getLogger(__name__)
router = APIRouter(tags=["websocket"])


async def get_ws_manager() -> WebSocketManager:
    """Dependency: get the global WebSocket manager."""
    # This will be overridden with the actual manager in main.py
    raise NotImplementedError("WebSocket manager not initialized")


@router.websocket("/ws")
async def websocket_endpoint(
    websocket: WebSocket,
    ws_manager: WebSocketManager = Depends(get_ws_manager),
):
    """WebSocket endpoint for Android devices to connect and receive SMS jobs."""
    await websocket.accept()
    logger.info(f"WebSocket connection accepted from {websocket.client}")

    device_info = None
    try:
        # Wait for device_info message
        data = await websocket.receive_text()
        message_data = json.loads(data)

        if message_data.get("type") != MessageType.DEVICE_INFO:
            error = ErrorMessage(
                code="INVALID_FIRST_MESSAGE",
                detail="First message must be device_info",
            )
            await websocket.send_json(error.model_dump(mode="json"))
            await websocket.close(code=1002, reason="Invalid first message")
            return

        device_info = DeviceInfoMessage(**message_data)
        logger.info(f"Device info received: {device_info.device_id}")

        # Register the device connection
        await ws_manager.connect(websocket, device_info)

        # Main message loop
        while True:
            data = await websocket.receive_text()
            message_data = json.loads(data)
            message_type = message_data.get("type")

            if message_type == MessageType.STATUS_UPDATE:
                status_msg = StatusUpdateMessage(**message_data)
                await ws_manager.handle_status_update(status_msg)

            elif message_type == MessageType.DEVICE_INFO:
                device_info_msg = DeviceInfoMessage(**message_data)
                await ws_manager.handle_device_info(device_info_msg)

            elif message_type == MessageType.PONG:
                pong_msg = PongMessage(**message_data)
                await ws_manager.handle_pong(pong_msg)

            elif message_type == MessageType.ERROR:
                error_msg = ErrorMessage(**message_data)
                logger.warning(
                    f"Error from device: {error_msg.code} - {error_msg.detail}"
                )

            else:
                logger.warning(f"Unknown message type: {message_type}")
                error = ErrorMessage(
                    code="UNKNOWN_MESSAGE_TYPE",
                    detail=f"Unknown message type: {message_type}",
                )
                await websocket.send_json(error.model_dump(mode="json"))

    except WebSocketDisconnect:
        logger.info(f"Device disconnected: {device_info.device_id if device_info else 'unknown'}")
        await ws_manager.disconnect(code=1000, reason="Client disconnected")

    except json.JSONDecodeError as e:
        logger.error(f"Invalid JSON received: {e}")
        await ws_manager.disconnect(code=1002, reason="Invalid JSON")

    except ValueError as e:
        logger.error(f"Message validation error: {e}")
        error = ErrorMessage(
            code="INVALID_MESSAGE",
            detail=str(e),
        )
        try:
            await websocket.send_json(error.model_dump(mode="json"))
        except:
            pass
        await ws_manager.disconnect(code=1002, reason="Invalid message")

    except Exception as e:
        logger.error(f"WebSocket error: {e}", exc_info=True)
        await ws_manager.disconnect(code=1011, reason="Server error")
