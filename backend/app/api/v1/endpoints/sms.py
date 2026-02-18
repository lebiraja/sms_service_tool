import logging
from fastapi import APIRouter, HTTPException, Query, Depends
from datetime import datetime

from app.models.sms_job import (
    SmsJobRequest,
    SmsJobResponse,
    SmsJobDetailResponse,
    SmsJobListResponse,
    SmsJobStatus,
)
from app.services.job_queue import JobQueue
from app.services.ws_manager import WebSocketManager

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/sms", tags=["sms"])


async def get_queue() -> JobQueue:
    """Dependency: get the global job queue."""
    # This will be overridden with the actual queue in main.py
    raise NotImplementedError("Queue not initialized")


async def get_ws_manager() -> WebSocketManager:
    """Dependency: get the global WebSocket manager."""
    # This will be overridden with the actual manager in main.py
    raise NotImplementedError("WebSocket manager not initialized")


@router.post("/send", status_code=202, response_model=SmsJobResponse)
async def send_sms(
    request: SmsJobRequest,
    queue: JobQueue = Depends(get_queue),
    ws_manager: WebSocketManager = Depends(get_ws_manager),
):
    """Submit a new SMS job to be sent via connected Android device.

    Returns 202 Accepted with the job_id.
    Returns 503 if no device is currently connected.
    Returns 429 if job queue is at capacity.
    """
    # Check if device is connected
    if not ws_manager.is_connected():
        raise HTTPException(
            status_code=503,
            detail={
                "error": "DEVICE_NOT_CONNECTED",
                "detail": "No Android device is currently connected to the gateway",
            },
        )

    try:
        job = await queue.create_job(
            to=request.to,
            body=request.body,
            max_retries=request.max_retries,
        )
    except ValueError as e:
        if "queue is at capacity" in str(e):
            raise HTTPException(
                status_code=429,
                detail={
                    "error": "QUEUE_FULL",
                    "detail": "Job queue is at capacity (max {})".format(1000),
                },
            )
        raise

    # Try to dispatch job to the connected device
    try:
        await ws_manager.send_job(job)
        logger.info(f"SMS job {job.job_id} dispatched to device")
    except Exception as e:
        logger.error(f"Failed to dispatch job {job.job_id}: {e}")
        # Job was created but not dispatched; client can retry
        raise HTTPException(
            status_code=503,
            detail={
                "error": "DISPATCH_FAILED",
                "detail": "Failed to dispatch job to device",
            },
        )

    return SmsJobResponse(
        job_id=job.job_id,
        status=job.status,
        created_at=job.created_at,
    )


@router.get("/jobs/{job_id}", response_model=SmsJobDetailResponse)
async def get_job(job_id: str, queue: JobQueue = Depends(get_queue)):
    """Get the current status of an SMS job."""
    job = await queue.get_job(job_id)
    if not job:
        raise HTTPException(status_code=404, detail={"error": "JOB_NOT_FOUND", "detail": f"No job with id: {job_id}"})
    return job.model_dump()


@router.get("/jobs", response_model=SmsJobListResponse)
async def list_jobs(
    queue: JobQueue = Depends(get_queue),
    status: str = Query(None, description="Filter by status"),
    limit: int = Query(50, ge=1, le=500),
    offset: int = Query(0, ge=0),
):
    """List recent SMS jobs with optional filtering."""
    status_filter = None
    if status:
        try:
            status_filter = SmsJobStatus(status)
        except ValueError:
            raise HTTPException(
                status_code=400,
                detail=f"Invalid status: {status}. Must be one of {[s.value for s in SmsJobStatus]}",
            )

    total, jobs = await queue.list_jobs(status_filter, limit, offset)
    job_dicts = [job.model_dump() for job in jobs]
    return SmsJobListResponse(
        total=total,
        limit=limit,
        offset=offset,
        jobs=job_dicts,
    )
