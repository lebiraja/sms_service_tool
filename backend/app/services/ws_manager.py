import asyncio
import logging
from datetime import datetime
from typing import Optional, Callable, Any
from fastapi import WebSocket

from app.models.message import (
    MessageType,
    SmsJobMessage,
    StatusUpdateMessage,
    DeviceInfoMessage,
    PingMessage,
    PongMessage,
    ErrorMessage,
)
from app.models.sms_job import SmsJobRecord, SmsJobStatus

logger = logging.getLogger(__name__)


class DeviceConnection:
    """Represents a connected device."""

    def __init__(self, websocket: WebSocket, device_info: DeviceInfoMessage):
        self.websocket = websocket
        self.device_id = device_info.device_id
        self.device_name = device_info.device_name
        self.android_version = device_info.android_version
        self.app_version = device_info.app_version
        self.sim_operator = device_info.sim_operator
        self.sim_country = device_info.sim_country
        self.connected_at = device_info.connected_at
        self.last_ping_at = datetime.utcnow()
        self.pending_pong_id: Optional[str] = None

    async def send_message(self, message: dict) -> bool:
        """Send a JSON message to the device. Returns True on success."""
        try:
            await self.websocket.send_json(message)
            return True
        except Exception as e:
            logger.error(f"Failed to send message to device {self.device_id}: {e}")
            return False

    async def close(self, code: int = 1000, reason: str = "Normal closure"):
        """Close the WebSocket connection."""
        try:
            await self.websocket.close(code=code, reason=reason)
        except Exception as e:
            logger.error(f"Error closing WebSocket: {e}")


class WebSocketManager:
    """Manages WebSocket connections with Android devices."""

    def __init__(self, ping_interval: int = 30, ping_timeout: int = 10):
        self.device: Optional[DeviceConnection] = None
        self.ping_interval = ping_interval
        self.ping_timeout = ping_timeout
        self._ping_task: Optional[asyncio.Task] = None
        self._status_callbacks: dict[str, Callable] = {}

    def is_connected(self) -> bool:
        """Check if a device is currently connected."""
        return self.device is not None

    def get_device_info(self) -> Optional[dict]:
        """Get current device connection info."""
        if not self.device:
            return None
        return {
            "device_id": self.device.device_id,
            "device_name": self.device.device_name,
            "android_version": self.device.android_version,
            "app_version": self.device.app_version,
            "sim_operator": self.device.sim_operator,
            "sim_country": self.device.sim_country,
            "connected_at": self.device.connected_at,
            "last_ping_at": self.device.last_ping_at,
        }

    async def connect(
        self,
        websocket: WebSocket,
        device_info: DeviceInfoMessage,
    ) -> None:
        """Establish a device connection and start the ping loop."""
        # Disconnect any existing device
        if self.device:
            logger.warning(
                f"Disconnecting existing device {self.device.device_id} "
                f"for new connection from {device_info.device_id}"
            )
            await self.disconnect(code=1000, reason="New device connected")

        self.device = DeviceConnection(websocket, device_info)
        logger.info(
            f"Device connected: {self.device.device_id} "
            f"({self.device.device_name}, Android {self.device.android_version})"
        )

        # Start ping loop
        self._ping_task = asyncio.create_task(self._ping_loop())

    async def disconnect(self, code: int = 1000, reason: str = "Disconnected") -> None:
        """Disconnect the current device."""
        if not self.device:
            return

        device_id = self.device.device_id
        self.device = None

        # Cancel ping loop
        if self._ping_task:
            self._ping_task.cancel()
            self._ping_task = None

        logger.info(f"Device disconnected: {device_id} ({reason})")

    async def send_job(self, job: SmsJobRecord) -> bool:
        """Send an SMS job to the connected device. Returns True on success."""
        if not self.device:
            raise RuntimeError("No device connected")

        message = SmsJobMessage(
            job_id=job.job_id,
            to=job.to,
            body=job.body,
            max_retries=job.max_retries,
            created_at=job.created_at,
        )

        logger.info(f"Dispatching job {job.job_id} to device {self.device.device_id}")
        return await self.device.send_message(message.model_dump(mode="json"))

    async def handle_status_update(
        self, message: StatusUpdateMessage
    ) -> Optional[dict]:
        """Handle a status update from the device."""
        logger.info(
            f"Status update for job {message.job_id}: {message.status} "
            f"(attempt {message.attempt})"
        )

        # Call any registered callbacks for this job
        if message.job_id in self._status_callbacks:
            callback = self._status_callbacks[message.job_id]
            try:
                await callback(message)
            except Exception as e:
                logger.error(f"Error in status callback: {e}")

        return {
            "job_id": message.job_id,
            "status": message.status,
            "acknowledged": True,
        }

    async def handle_device_info(self, message: DeviceInfoMessage) -> None:
        """Handle device info message (usually on reconnect)."""
        if not self.device:
            logger.warning("Received device_info but no device connected")
            return

        logger.info(
            f"Device info update: {message.device_id} ({message.device_name})"
        )
        # Update device info if needed
        self.device.device_name = message.device_name
        self.device.sim_operator = message.sim_operator
        self.device.sim_country = message.sim_country

    async def handle_pong(self, message: PongMessage) -> None:
        """Handle a pong response from the device."""
        if self.device:
            self.device.last_ping_at = datetime.utcnow()
            self.device.pending_pong_id = None
            logger.debug(f"Pong received from device {self.device.device_id}")

    async def _ping_loop(self) -> None:
        """Periodically send pings to the device and check for pong responses."""
        if not self.device:
            return

        try:
            while self.device:
                await asyncio.sleep(self.ping_interval)

                if not self.device:
                    break

                # Send ping
                ping_msg = PingMessage()
                self.device.pending_pong_id = ping_msg.message_id

                if not await self.device.send_message(ping_msg.model_dump(mode="json")):
                    logger.warning(
                        f"Failed to send ping to device {self.device.device_id}"
                    )
                    break

                # Wait for pong with timeout
                pong_received = False
                for _ in range(self.ping_timeout):
                    await asyncio.sleep(1)
                    if self.device and self.device.pending_pong_id is None:
                        pong_received = True
                        break

                if not pong_received and self.device:
                    logger.warning(
                        f"No pong received from device {self.device.device_id} "
                        f"within {self.ping_timeout}s, disconnecting"
                    )
                    await self.disconnect(
                        code=1000, reason="Ping timeout"
                    )
                    break

        except asyncio.CancelledError:
            logger.debug("Ping loop cancelled")
        except Exception as e:
            logger.error(f"Error in ping loop: {e}")
            if self.device:
                await self.disconnect(code=1011, reason="Ping loop error")

    def register_status_callback(
        self, job_id: str, callback: Callable[[StatusUpdateMessage], Any]
    ) -> None:
        """Register a callback for status updates for a specific job."""
        self._status_callbacks[job_id] = callback

    def unregister_status_callback(self, job_id: str) -> None:
        """Unregister a status callback."""
        self._status_callbacks.pop(job_id, None)
