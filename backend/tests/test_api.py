import pytest
from fastapi.testclient import TestClient
from datetime import datetime
from unittest.mock import AsyncMock, MagicMock, patch

from app.main import create_app, job_queue, ws_manager
from app.models.sms_job import SmsJobStatus
from app.models.message import DeviceInfoMessage


@pytest.fixture
def client():
    """Create a test client."""
    app = create_app()
    return TestClient(app)


@pytest.fixture
async def reset_queue():
    """Reset the job queue before each test."""
    global job_queue
    job_queue._jobs.clear()
    yield
    job_queue._jobs.clear()


@pytest.fixture
async def mock_device_connected():
    """Mock a connected device for tests that need it."""
    global ws_manager

    # Create a mock device
    mock_websocket = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="test-msg",
        device_id="test-device-123",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    # Connect the mock device
    await ws_manager.connect(mock_websocket, device_info)

    yield

    # Disconnect after test
    await ws_manager.disconnect()


def test_health_check(client):
    """Test the health check endpoint."""
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "version" in data


def test_root_endpoint(client):
    """Test the root endpoint."""
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "version" in data


@pytest.mark.asyncio
async def test_send_sms(client, reset_queue, mock_device_connected):
    """Test sending an SMS."""
    response = client.post(
        "/api/v1/sms/send",
        json={
            "to": "+15551234567",
            "body": "Test message",
            "max_retries": 3,
        }
    )
    assert response.status_code == 202
    data = response.json()
    assert data["job_id"] is not None
    assert data["status"] == "queued"


@pytest.mark.asyncio
async def test_get_job(client, reset_queue, mock_device_connected):
    """Test getting a job by ID."""
    # First, create a job
    send_response = client.post(
        "/api/v1/sms/send",
        json={
            "to": "+15551234567",
            "body": "Test message",
        }
    )
    assert send_response.status_code == 202
    job_id = send_response.json()["job_id"]

    # Now retrieve it
    get_response = client.get(f"/api/v1/sms/jobs/{job_id}")
    assert get_response.status_code == 200
    data = get_response.json()
    assert data["job_id"] == job_id
    assert data["to"] == "+15551234567"
    assert data["body"] == "Test message"


@pytest.mark.asyncio
async def test_get_nonexistent_job(client):
    """Test getting a job that doesn't exist."""
    response = client.get("/api/v1/sms/jobs/nonexistent-id")
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_list_jobs(client, reset_queue, mock_device_connected):
    """Test listing jobs."""
    # Create a few jobs
    for i in range(3):
        client.post(
            "/api/v1/sms/send",
            json={
                "to": f"+1555123456{i}",
                "body": f"Test {i}",
            }
        )

    # List all jobs
    response = client.get("/api/v1/sms/jobs")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 3
    assert len(data["jobs"]) == 3


@pytest.mark.asyncio
async def test_list_jobs_pagination(client, reset_queue, mock_device_connected):
    """Test listing jobs with pagination."""
    # Create 10 jobs
    for i in range(10):
        client.post(
            "/api/v1/sms/send",
            json={
                "to": f"+1555{i:010d}",
                "body": f"Test {i}",
            }
        )

    # Get first page
    response = client.get("/api/v1/sms/jobs?limit=3&offset=0")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 10
    assert len(data["jobs"]) == 3
    assert data["limit"] == 3
    assert data["offset"] == 0


def test_device_status_not_connected(client):
    """Test device status when no device is connected."""
    response = client.get("/api/v1/device/status")
    assert response.status_code == 200
    data = response.json()
    assert data["connected"] is False


@pytest.mark.asyncio
async def test_device_status_connected(client, mock_device_connected):
    """Test device status when a device is connected."""
    response = client.get("/api/v1/device/status")
    assert response.status_code == 200
    data = response.json()
    assert data["connected"] is True
    assert data["device_id"] == "test-device-123"
    assert data["device_name"] == "Test Device"


def test_send_sms_no_device(client, reset_queue):
    """Test sending SMS when no device is connected."""
    response = client.post(
        "/api/v1/sms/send",
        json={
            "to": "+15551234567",
            "body": "Test message",
        }
    )
    assert response.status_code == 503
    data = response.json()
    assert data["detail"]["error"] == "DEVICE_NOT_CONNECTED"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
