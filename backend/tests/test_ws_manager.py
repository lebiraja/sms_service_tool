import pytest
import asyncio
from datetime import datetime
from unittest.mock import AsyncMock

from app.services.ws_manager import WebSocketManager, DeviceConnection
from app.models.message import (
    DeviceInfoMessage,
    StatusUpdateMessage,
    PongMessage,
)
from app.models.sms_job import SmsJobRecord, SmsJobStatus


@pytest.fixture
def ws_manager():
    """Create a WebSocket manager for testing."""
    return WebSocketManager(ping_interval=1, ping_timeout=2)


@pytest.mark.asyncio
async def test_device_connection_init():
    """Test DeviceConnection initialization."""
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    conn = DeviceConnection(mock_ws, device_info)
    assert conn.device_id == "device-1"
    assert conn.device_name == "Test Device"
    assert conn.pending_pong_id is None


@pytest.mark.asyncio
async def test_is_connected(ws_manager):
    """Test is_connected() when no device is connected."""
    assert not ws_manager.is_connected()


@pytest.mark.asyncio
async def test_connect_device(ws_manager):
    """Test connecting a device."""
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)

    assert ws_manager.is_connected()
    assert ws_manager.device is not None
    assert ws_manager.device.device_id == "device-1"


@pytest.mark.asyncio
async def test_get_device_info(ws_manager):
    """Test getting device info."""
    assert ws_manager.get_device_info() is None

    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)

    info = ws_manager.get_device_info()
    assert info is not None
    assert info["device_id"] == "device-1"
    assert info["device_name"] == "Test Device"


@pytest.mark.asyncio
async def test_send_job(ws_manager):
    """Test sending a job to a device."""
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)

    job = SmsJobRecord(
        job_id="job-1",
        to="+15551234567",
        body="Test message",
        status=SmsJobStatus.QUEUED,
        created_at=datetime.utcnow(),
    )

    result = await ws_manager.send_job(job)
    assert result is True
    mock_ws.send_json.assert_called_once()


@pytest.mark.asyncio
async def test_send_job_no_device(ws_manager):
    """Test sending a job when no device is connected."""
    job = SmsJobRecord(
        job_id="job-1",
        to="+15551234567",
        body="Test message",
        status=SmsJobStatus.QUEUED,
        created_at=datetime.utcnow(),
    )

    with pytest.raises(RuntimeError, match="No device connected"):
        await ws_manager.send_job(job)


@pytest.mark.asyncio
async def test_handle_status_update(ws_manager):
    """Test handling a status update from device."""
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)

    status_msg = StatusUpdateMessage(
        message_id="msg-2",
        job_id="job-1",
        status=SmsJobStatus.SENT,
        attempt=1,
        timestamp=datetime.utcnow(),
    )

    result = await ws_manager.handle_status_update(status_msg)
    assert result["job_id"] == "job-1"
    assert result["acknowledged"] is True


@pytest.mark.asyncio
async def test_handle_pong(ws_manager):
    """Test handling a pong from device."""
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)
    original_ping_id = "ping-123"
    ws_manager.device.pending_pong_id = original_ping_id

    pong_msg = PongMessage(
        message_id="pong-1",
        ping_message_id=original_ping_id,
    )

    await ws_manager.handle_pong(pong_msg)

    # Check that pending_pong_id was cleared
    assert ws_manager.device.pending_pong_id is None


@pytest.mark.asyncio
async def test_disconnect(ws_manager):
    """Test disconnecting a device."""
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)
    assert ws_manager.is_connected()

    await ws_manager.disconnect()
    assert not ws_manager.is_connected()


@pytest.mark.asyncio
async def test_status_callbacks(ws_manager):
    """Test registering and calling status callbacks."""
    callback_called = False
    received_message = None

    async def test_callback(msg):
        nonlocal callback_called, received_message
        callback_called = True
        received_message = msg

    # Register callback
    ws_manager.register_status_callback("job-1", test_callback)

    # Create a mock device
    mock_ws = AsyncMock()
    device_info = DeviceInfoMessage(
        message_id="msg-1",
        device_id="device-1",
        device_name="Test Device",
        android_version="14",
        app_version="1.0.0",
        connected_at=datetime.utcnow(),
    )

    await ws_manager.connect(mock_ws, device_info)

    # Send a status update
    status_msg = StatusUpdateMessage(
        message_id="msg-2",
        job_id="job-1",
        status=SmsJobStatus.SENT,
        attempt=1,
        timestamp=datetime.utcnow(),
    )

    await ws_manager.handle_status_update(status_msg)

    # Verify callback was called
    assert callback_called is True
    assert received_message.job_id == "job-1"

    # Unregister callback
    ws_manager.unregister_status_callback("job-1")
    assert "job-1" not in ws_manager._status_callbacks


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
