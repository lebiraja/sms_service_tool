from enum import Enum
from datetime import datetime
from typing import Optional, Literal
from uuid import uuid4
from pydantic import BaseModel, Field

from .sms_job import SmsJobStatus


class MessageType(str, Enum):
    """WebSocket message types."""
    SMS_JOB = "sms_job"
    STATUS_UPDATE = "status_update"
    DEVICE_INFO = "device_info"
    PING = "ping"
    PONG = "pong"
    ERROR = "error"


class SmsJobMessage(BaseModel):
    """Server → Android: dispatch an SMS job."""
    type: Literal[MessageType.SMS_JOB] = MessageType.SMS_JOB
    message_id: str = Field(default_factory=lambda: str(uuid4()))
    job_id: str
    to: str
    body: str
    max_retries: int = 3
    created_at: datetime


class StatusUpdateMessage(BaseModel):
    """Android → Server: status update on an SMS job."""
    type: Literal[MessageType.STATUS_UPDATE] = MessageType.STATUS_UPDATE
    message_id: str
    job_id: str
    status: SmsJobStatus
    attempt: int
    error_code: Optional[int] = None
    error_message: Optional[str] = None
    timestamp: datetime


class DeviceInfoMessage(BaseModel):
    """Android → Server: device info on connect."""
    type: Literal[MessageType.DEVICE_INFO] = MessageType.DEVICE_INFO
    message_id: str
    device_id: str
    device_name: str
    android_version: str
    app_version: str
    sim_operator: Optional[str] = None
    sim_country: Optional[str] = None
    connected_at: datetime


class PingMessage(BaseModel):
    """Server → Android: keepalive ping."""
    type: Literal[MessageType.PING] = MessageType.PING
    message_id: str = Field(default_factory=lambda: str(uuid4()))
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class PongMessage(BaseModel):
    """Android → Server: keepalive pong."""
    type: Literal[MessageType.PONG] = MessageType.PONG
    message_id: str
    ping_message_id: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class ErrorMessage(BaseModel):
    """Bidirectional: protocol error."""
    type: Literal[MessageType.ERROR] = MessageType.ERROR
    message_id: str = Field(default_factory=lambda: str(uuid4()))
    ref_message_id: Optional[str] = None
    code: str
    detail: str


# Union type for all inbound messages from Android
InboundMessage = StatusUpdateMessage | DeviceInfoMessage | PongMessage | ErrorMessage

# Union type for all outbound messages to Android
OutboundMessage = SmsJobMessage | PingMessage | ErrorMessage
