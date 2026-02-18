import pytest
from datetime import datetime

from app.models.sms_job import (
    SmsJobStatus,
    SmsJobRecord,
    SmsJobRequest,
    SmsJobResponse,
    SmsJobDetailResponse,
)
from app.models.message import (
    MessageType,
    SmsJobMessage,
    StatusUpdateMessage,
    DeviceInfoMessage,
    PingMessage,
    PongMessage,
)


class TestSmsJobModels:
    """Test SMS job model validation."""

    def test_sms_job_request_valid(self):
        """Test valid SMS job request."""
        request = SmsJobRequest(
            to="+15551234567",
            body="Hello world",
        )
        assert request.to == "+15551234567"
        assert request.body == "Hello world"
        assert request.max_retries == 3

    def test_sms_job_request_with_custom_retries(self):
        """Test SMS job request with custom retry count."""
        request = SmsJobRequest(
            to="+15551234567",
            body="Test",
            max_retries=5,
        )
        assert request.max_retries == 5

    def test_sms_job_request_invalid_retries(self):
        """Test SMS job request with invalid retry count."""
        with pytest.raises(ValueError):
            SmsJobRequest(
                to="+15551234567",
                body="Test",
                max_retries=15,  # Max is 10
            )

    def test_sms_job_record(self):
        """Test SMS job record creation."""
        now = datetime.utcnow()
        job = SmsJobRecord(
            job_id="test-id",
            to="+15551234567",
            body="Test message",
            status=SmsJobStatus.QUEUED,
            created_at=now,
        )
        assert job.job_id == "test-id"
        assert job.status == SmsJobStatus.QUEUED
        assert job.attempts == 0


class TestMessageModels:
    """Test WebSocket message model validation."""

    def test_sms_job_message(self):
        """Test SMS job message creation."""
        now = datetime.utcnow()
        msg = SmsJobMessage(
            job_id="job-123",
            to="+15551234567",
            body="Test SMS",
            created_at=now,
        )
        assert msg.type == MessageType.SMS_JOB
        assert msg.job_id == "job-123"
        assert msg.message_id is not None

    def test_status_update_message(self):
        """Test status update message creation."""
        now = datetime.utcnow()
        msg = StatusUpdateMessage(
            message_id="msg-123",
            job_id="job-123",
            status=SmsJobStatus.SENT,
            attempt=1,
            timestamp=now,
        )
        assert msg.type == MessageType.STATUS_UPDATE
        assert msg.status == SmsJobStatus.SENT

    def test_device_info_message(self):
        """Test device info message creation."""
        now = datetime.utcnow()
        msg = DeviceInfoMessage(
            message_id="msg-123",
            device_id="device-123",
            device_name="Pixel 7 Pro",
            android_version="14",
            app_version="1.0.0",
            connected_at=now,
        )
        assert msg.type == MessageType.DEVICE_INFO
        assert msg.device_name == "Pixel 7 Pro"

    def test_ping_message(self):
        """Test ping message creation."""
        msg = PingMessage()
        assert msg.type == MessageType.PING
        assert msg.message_id is not None

    def test_pong_message(self):
        """Test pong message creation."""
        msg = PongMessage(
            message_id="pong-123",
            ping_message_id="ping-123",
        )
        assert msg.type == MessageType.PONG
        assert msg.ping_message_id == "ping-123"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
