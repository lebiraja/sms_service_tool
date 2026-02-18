import logging
from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from datetime import datetime
from typing import Optional

from app.models.sms_job import SmsJobRecord, SmsJobStatus
from app.services.job_queue import JobQueue

logger = logging.getLogger(__name__)
router = APIRouter(tags=["device"])

# Store connected devices: {device_id: {info, last_seen}}
# A device is considered connected if it has polled within the last 30 seconds
connected_devices = {}


def is_any_device_connected() -> bool:
    """Check if any device is currently connected (polled within last 30 seconds)."""
    from datetime import datetime, timedelta
    now = datetime.utcnow()
    for device_id, info in connected_devices.items():
        last_seen = info.get("last_seen")
        if last_seen:
            try:
                last_seen_dt = datetime.fromisoformat(last_seen)
                if (now - last_seen_dt).total_seconds() < 30:
                    return True
            except (ValueError, TypeError):
                pass
    return False


def get_connected_devices():
    """Get list of currently connected devices."""
    from datetime import datetime, timedelta
    now = datetime.utcnow()
    connected = []
    for device_id, info in list(connected_devices.items()):
        last_seen = info.get("last_seen")
        if last_seen:
            try:
                last_seen_dt = datetime.fromisoformat(last_seen)
                if (now - last_seen_dt).total_seconds() < 30:
                    connected.append(device_id)
            except (ValueError, TypeError):
                pass
    return connected


async def get_queue() -> JobQueue:
    """Dependency: get the global job queue."""
    raise NotImplementedError("Queue not initialized")


class DeviceInfoRequest(BaseModel):
    """Device info registration request."""
    device_id: str
    device_name: Optional[str] = None
    app_version: Optional[str] = None


class StatusUpdateRequest(BaseModel):
    """Job status update request."""
    status: str
    attempt: int
    error_code: Optional[int] = None
    error_message: Optional[str] = None
    timestamp: Optional[str] = None


@router.post("/device/info", tags=["device-polling"])
async def register_device(
    request: DeviceInfoRequest,
    queue: JobQueue = Depends(get_queue),
):
    """Register/update a device connection.

    The device sends this periodically to indicate it's alive and connected.
    """
    connected_devices[request.device_id] = {
        "device_id": request.device_id,
        "device_name": request.device_name,
        "app_version": request.app_version,
        "last_seen": datetime.utcnow().isoformat(),
    }
    logger.info(f"✓ Device REGISTERED: {request.device_id} | Name: {request.device_name} | App: {request.app_version} | Total devices: {len(connected_devices)}")
    return {
        "status": "registered",
        "device_id": request.device_id,
        "timestamp": datetime.utcnow().isoformat(),
    }


@router.get("/jobs/pending")
async def get_pending_jobs(
    device_id: str = Query(..., description="Device ID"),
    queue: JobQueue = Depends(get_queue),
):
    """Poll for pending SMS jobs for a specific device.

    Returns all jobs with status QUEUED that haven't been sent to this device yet.
    """
    # Update device last_seen to keep it marked as connected
    if device_id in connected_devices:
        connected_devices[device_id]["last_seen"] = datetime.utcnow().isoformat()
        logger.info(f"✓ Device {device_id} polled (connection refreshed)")
    else:
        logger.info(f"⚠ Unknown device polling: {device_id}")

    # Get all queued jobs
    _, jobs = await queue.list_jobs(SmsJobStatus.QUEUED, limit=100, offset=0)

    # Convert to list of dicts for JSON response
    job_list = []
    for job in jobs:
        job_list.append({
            "jobId": job.job_id,
            "toNumber": job.to,
            "body": job.body,
            "maxRetries": job.max_retries,
        })

    if job_list:
        logger.info(f"  → Found {len(job_list)} pending job(s) for {device_id}")
        for job in job_list:
            logger.info(f"    ↳ Job {job['jobId']} to {job['toNumber']}: {job['body'][:40]}...")

    return job_list


@router.post("/jobs/{job_id}/status")
async def report_job_status(
    job_id: str,
    request: StatusUpdateRequest,
    device_id: str = Query(..., description="Device ID"),
    queue: JobQueue = Depends(get_queue),
):
    """Report the status of an SMS job back to the server.

    The device sends updates as it processes the job (sending, sent, delivered, failed).
    """
    try:
        # Map Flutter status to backend status
        status_map = {
            "queued": SmsJobStatus.QUEUED,
            "sending": SmsJobStatus.SENDING,
            "sent": SmsJobStatus.SENT,
            "delivered": SmsJobStatus.DELIVERED,
            "failed_retrying": SmsJobStatus.FAILED_RETRYING,
            "failed_permanent": SmsJobStatus.FAILED_PERMANENT,
        }

        job_status = status_map.get(request.status)
        if not job_status:
            raise HTTPException(status_code=400, detail=f"Invalid status: {request.status}")

        # Get the job and update it
        job = await queue.get_job(job_id)
        if not job:
            logger.warning(f"Job {job_id} not found for status update from {device_id}")
            return {"status": "job_not_found", "job_id": job_id}

        # Update job status
        job.status = job_status
        job.attempts = request.attempt
        if request.error_code is not None:
            job.error_code = request.error_code
        if request.error_message:
            job.error_message = request.error_message

        # Update timestamps based on status
        if job_status == SmsJobStatus.SENT:
            job.sent_at = datetime.fromisoformat(request.timestamp) if request.timestamp else datetime.utcnow()
        elif job_status == SmsJobStatus.DELIVERED:
            job.delivered_at = datetime.fromisoformat(request.timestamp) if request.timestamp else datetime.utcnow()

        logger.info(f"Job {job_id} status updated to {request.status} by device {device_id}")

        return {
            "status": "updated",
            "job_id": job_id,
            "job_status": request.status,
            "timestamp": datetime.utcnow().isoformat(),
        }
    except Exception as e:
        logger.error(f"Error updating job {job_id} status: {e}")
        raise HTTPException(status_code=500, detail=str(e))
