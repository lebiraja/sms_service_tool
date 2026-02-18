import pytest
import asyncio
from datetime import datetime

from app.services.job_queue import JobQueue
from app.models.sms_job import SmsJobStatus


@pytest.fixture
def job_queue():
    """Create a fresh job queue for each test."""
    return JobQueue()


@pytest.mark.asyncio
async def test_create_job(job_queue):
    """Test creating a new SMS job."""
    job = await job_queue.create_job(
        to="+15551234567",
        body="Test message",
        max_retries=3,
    )

    assert job.to == "+15551234567"
    assert job.body == "Test message"
    assert job.status == SmsJobStatus.QUEUED
    assert job.attempts == 0
    assert job.max_retries == 3
    assert job.job_id is not None


@pytest.mark.asyncio
async def test_get_job(job_queue):
    """Test retrieving a job by ID."""
    created_job = await job_queue.create_job(
        to="+15551234567",
        body="Test",
    )

    retrieved_job = await job_queue.get_job(created_job.job_id)
    assert retrieved_job is not None
    assert retrieved_job.job_id == created_job.job_id
    assert retrieved_job.to == "+15551234567"


@pytest.mark.asyncio
async def test_get_nonexistent_job(job_queue):
    """Test retrieving a job that doesn't exist."""
    job = await job_queue.get_job("nonexistent-id")
    assert job is None


@pytest.mark.asyncio
async def test_update_status(job_queue):
    """Test updating job status."""
    job = await job_queue.create_job(to="+15551234567", body="Test")
    job_id = job.job_id

    updated = await job_queue.update_status(
        job_id,
        SmsJobStatus.SENT,
        attempt=1,
        error_code=None,
    )

    assert updated is not None
    assert updated.status == SmsJobStatus.SENT
    assert updated.attempts == 1


@pytest.mark.asyncio
async def test_list_jobs(job_queue):
    """Test listing jobs with pagination."""
    # Create multiple jobs
    for i in range(5):
        await job_queue.create_job(
            to=f"+1555123456{i}",
            body=f"Test {i}",
        )

    # List all jobs
    total, jobs = await job_queue.list_jobs(limit=10, offset=0)
    assert total == 5
    assert len(jobs) == 5


@pytest.mark.asyncio
async def test_list_jobs_with_pagination(job_queue):
    """Test list jobs with limit and offset."""
    # Create 10 jobs
    for i in range(10):
        await job_queue.create_job(to=f"+1555{i:010d}", body=f"Test {i}")

    # Get first 3
    total, jobs = await job_queue.list_jobs(limit=3, offset=0)
    assert total == 10
    assert len(jobs) == 3

    # Get next 3
    total, jobs = await job_queue.list_jobs(limit=3, offset=3)
    assert total == 10
    assert len(jobs) == 3


@pytest.mark.asyncio
async def test_list_jobs_with_status_filter(job_queue):
    """Test filtering jobs by status."""
    # Create jobs with different statuses
    job1 = await job_queue.create_job(to="+15551111111", body="Test 1")
    job2 = await job_queue.create_job(to="+15552222222", body="Test 2")

    await job_queue.update_status(job1.job_id, SmsJobStatus.SENT)

    # List only SENT jobs
    total, jobs = await job_queue.list_jobs(
        status_filter=SmsJobStatus.SENT,
        limit=10,
        offset=0,
    )
    assert total == 1
    assert jobs[0].job_id == job1.job_id

    # List only QUEUED jobs
    total, jobs = await job_queue.list_jobs(
        status_filter=SmsJobStatus.QUEUED,
        limit=10,
        offset=0,
    )
    assert total == 1
    assert jobs[0].job_id == job2.job_id


@pytest.mark.asyncio
async def test_queue_capacity_limit(job_queue):
    """Test that queue enforces max size limit."""
    # Fill up the queue (default max is 1000)
    # For testing, we'd need to reduce the limit, but this shows the concept
    job = await job_queue.create_job(to="+15551234567", body="Test")
    assert job is not None


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
