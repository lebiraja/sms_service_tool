from enum import Enum
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, ConfigDict


class SmsJobStatus(str, Enum):
    """SMS job status enum."""
    QUEUED = "queued"
    SENDING = "sending"
    SENT = "sent"
    DELIVERED = "delivered"
    FAILED_RETRYING = "failed_retrying"
    FAILED_PERMANENT = "failed_permanent"


class SmsJobRecord(BaseModel):
    """In-memory SMS job record."""
    model_config = ConfigDict(use_enum_values=False)

    job_id: str
    to: str
    body: str
    status: SmsJobStatus
    attempts: int = 0
    max_retries: int = 3
    created_at: datetime
    sent_at: Optional[datetime] = None
    delivered_at: Optional[datetime] = None
    error_code: Optional[int] = None
    error_message: Optional[str] = None


class SmsJobRequest(BaseModel):
    """REST request body for submitting an SMS job."""
    to: str = Field(..., description="E.164 formatted phone number")
    body: str = Field(..., description="SMS message body (1-1600 chars)")
    max_retries: int = Field(3, ge=0, le=10, description="Number of retries")


class SmsJobResponse(BaseModel):
    """REST response for job submission."""
    job_id: str
    status: SmsJobStatus
    created_at: datetime


class SmsJobDetailResponse(BaseModel):
    """REST response with full job details."""
    job_id: str
    to: str
    body: str
    status: SmsJobStatus
    attempts: int
    max_retries: int
    created_at: datetime
    sent_at: Optional[datetime] = None
    delivered_at: Optional[datetime] = None
    error_code: Optional[int] = None
    error_message: Optional[str] = None


class SmsJobListResponse(BaseModel):
    """Paginated job list response."""
    total: int
    limit: int
    offset: int
    jobs: list[SmsJobDetailResponse]
