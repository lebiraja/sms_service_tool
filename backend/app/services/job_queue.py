import asyncio
from datetime import datetime
from typing import Optional, Tuple
from uuid import uuid4

from app.models.sms_job import SmsJobRecord, SmsJobStatus
from app.config import settings


class JobQueue:
    """In-memory job queue with optional SQLite persistence."""

    def __init__(self):
        self._jobs: dict[str, SmsJobRecord] = {}
        self._lock = asyncio.Lock()

    async def create_job(
        self, to: str, body: str, max_retries: int = 3
    ) -> SmsJobRecord:
        """Create a new SMS job. Raises ValueError if queue is full."""
        async with self._lock:
            if len(self._jobs) >= settings.job_queue_max_size:
                raise ValueError("Job queue is at capacity")

            job_id = str(uuid4())
            now = datetime.utcnow()
            job = SmsJobRecord(
                job_id=job_id,
                to=to,
                body=body,
                status=SmsJobStatus.QUEUED,
                attempts=0,
                max_retries=max_retries,
                created_at=now,
            )
            self._jobs[job_id] = job
            return job

    async def update_status(
        self,
        job_id: str,
        status: SmsJobStatus,
        attempt: Optional[int] = None,
        error_code: Optional[int] = None,
        error_message: Optional[str] = None,
        sent_at: Optional[datetime] = None,
        delivered_at: Optional[datetime] = None,
    ) -> Optional[SmsJobRecord]:
        """Update job status. Returns updated job or None if not found."""
        async with self._lock:
            if job_id not in self._jobs:
                return None

            job = self._jobs[job_id]
            job.status = status

            if attempt is not None:
                job.attempts = attempt
            if error_code is not None:
                job.error_code = error_code
            if error_message is not None:
                job.error_message = error_message
            if sent_at is not None:
                job.sent_at = sent_at
            if delivered_at is not None:
                job.delivered_at = delivered_at

            return job

    async def get_job(self, job_id: str) -> Optional[SmsJobRecord]:
        """Get job by ID."""
        async with self._lock:
            return self._jobs.get(job_id)

    async def list_jobs(
        self,
        status_filter: Optional[SmsJobStatus] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> Tuple[int, list[SmsJobRecord]]:
        """List jobs with optional status filter. Returns (total_count, jobs)."""
        async with self._lock:
            jobs = list(self._jobs.values())

            # Filter by status if specified
            if status_filter:
                jobs = [j for j in jobs if j.status == status_filter]

            # Sort by created_at descending (newest first)
            jobs.sort(key=lambda j: j.created_at, reverse=True)

            total = len(jobs)
            paginated = jobs[offset : offset + limit]

            return total, paginated

    async def get_all_jobs(self) -> list[SmsJobRecord]:
        """Get all jobs (for debugging)."""
        async with self._lock:
            return list(self._jobs.values())
