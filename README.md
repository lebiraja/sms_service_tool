# SMSTool — Open Source Android SMS Gateway

> Turn any Android device into a self-hosted SMS gateway. Send SMS messages via REST API using the device's SIM card.

## How It Works

```
┌─────────────────────────────────────────────────────────────┐
│  REST Client / External System                              │
│  (POST /api/v1/sms/send with phone number + message)        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
         ┌─────────────────────────────┐
         │  Python Backend (FastAPI)   │
         │  ─ REST API                 │
         │  ─ Job Queue                │
         │  ─ WebSocket Manager        │
         └──────────────┬──────────────┘
                        │
              WebSocket │ (persistent connection)
                        │
         ┌──────────────▼──────────────┐
         │  Android App (Java)         │
         │  ─ Foreground Service       │
         │  ─ SMS Manager              │
         │  ─ Local Job Queue (Room DB)│
         └──────────────┬──────────────┘
                        │
                   SMS  │ (via device SIM)
                        │
         ┌──────────────▼──────────────┐
         │  Mobile Network              │
         │  ─ Delivers SMS to recipient │
         └─────────────────────────────┘
```

## Key Features

✨ **Zero Authentication** — URL is the shared secret. Perfect for private networks.

🔄 **Persistent WebSocket** — Always-on connection between device and backend.

📱 **Real Device SIM** — Uses the actual SIM card in the phone. No carrier accounts needed.

💾 **Local Retry Queue** — SMS queued locally on device. Survives app crashes and network outages.

📊 **Full Status Tracking** — Know when SMS is sent, delivered, or failed with detailed error codes.

🐳 **Docker Ready** — One-command deployment. Works on cheap VPS, Raspberry Pi, or home server.

⚙️ **Simple REST API** — Standard HTTP endpoints. Easy to integrate with existing systems.

---

## Quick Start

### Option 1: Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/yourusername/smstool.git
cd smstool/backend

# Start the backend
docker compose up -d

# Verify it's running
curl http://localhost:7777/api/v1/health
# Response: {"status":"ok","version":"1.0.0","device_connected":false}
```

### Option 2: Local Development

```bash
# Set up Python environment
cd backend
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run the server
uvicorn app.main:app --host 0.0.0.0 --port 7777

# In another terminal, run tests
pytest tests/ -v
```

### Android App Setup

1. **Download APK** from the [Releases](https://github.com/yourusername/smstool/releases) page
2. **Install** on your Android device (enable unknown sources if needed)
3. **Open the app**
4. **Enter backend URL**: `ws://your-server-ip:7777/ws`
5. **Tap "Connect"** — wait for green status indicator
6. **You're ready!** The device is now an SMS gateway

---

## API Usage

### Health Check

Check if the backend is running (used by Android app on startup):

```bash
curl http://localhost:7777/api/v1/health
```

Response:
```json
{
  "status": "ok",
  "version": "1.0.0",
  "device_connected": true
}
```

### Send SMS

Submit an SMS job. The backend will forward it to the connected Android device.

```bash
curl -X POST http://localhost:7777/api/v1/sms/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": "+15551234567",
    "body": "Hello from SMSTool!",
    "max_retries": 3
  }'
```

Response (202 Accepted):
```json
{
  "job_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "queued",
  "created_at": "2026-02-17T21:00:00Z"
}
```

### Get Job Status

Check the status of a sent SMS:

```bash
curl http://localhost:7777/api/v1/sms/jobs/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

Response:
```json
{
  "job_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "to": "+15551234567",
  "body": "Hello from SMSTool!",
  "status": "delivered",
  "attempts": 1,
  "max_retries": 3,
  "created_at": "2026-02-17T21:00:00Z",
  "sent_at": "2026-02-17T21:00:05Z",
  "delivered_at": "2026-02-17T21:00:08Z",
  "error_code": null,
  "error_message": null
}
```

Job statuses:
- `queued` — Job received, waiting to be sent
- `sending` — SMS manager is processing
- `sent` — Network accepted the SMS
- `delivered` — Recipient device received it
- `failed_retrying` — Send failed, will retry
- `failed_permanent` — All retries exhausted

### List Jobs

List recent SMS jobs with optional filtering:

```bash
curl "http://localhost:7777/api/v1/sms/jobs?status=sent&limit=10&offset=0"
```

Response:
```json
{
  "total": 42,
  "limit": 10,
  "offset": 0,
  "jobs": [...]
}
```

### Device Status

Check if an Android device is currently connected:

```bash
curl http://localhost:7777/api/v1/device/status
```

Response (device connected):
```json
{
  "connected": true,
  "device_id": "device-uuid",
  "device_name": "Pixel 7 Pro",
  "android_version": "14",
  "app_version": "1.0.0",
  "sim_operator": "T-Mobile",
  "connected_at": "2026-02-17T21:00:00Z",
  "last_ping_at": "2026-02-17T21:05:30Z"
}
```

---

## Architecture

### Backend (Python + FastAPI)

- **REST API** (`POST /api/v1/sms/send`, `GET /api/v1/sms/jobs/*`)
- **WebSocket** (`/api/v1/ws`) — Persistent connection with Android devices
- **Job Queue** — In-memory with optional SQLite persistence
- **Device Manager** — Tracks connected devices, manages ping/pong keepalive

### Android App (Java)

- **Foreground Service** — Keeps connection alive, survives app backgrounding
- **Room Database** — Local job queue (survives process death)
- **SMS Manager** — Native Android SMS sending
- **Broadcast Receivers** — SMS delivery reports
- **Retry Logic** — Exponential backoff, local queue resume on restart
- **MainActivity** — Single-screen UI showing connection status and job log

---

## Deployment

### Self-Host on VPS

```bash
# On your server (Ubuntu/Debian)
cd /opt/smstool
git clone https://github.com/yourusername/smstool.git .
cd backend
docker compose up -d
```

Access the backend:
- API: `http://your-server:7777`
- Docs: `http://your-server:7777/docs` (Swagger UI)

### Self-Host on Raspberry Pi

```bash
# Same as VPS, Docker works great on ARM
docker compose up -d

# Or use native Python if Docker is slow:
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 7777
```

### Android Device Setup

1. **Choose a dedicated phone** (or spare Android device)
2. **Insert SIM** with active SMS plan (even $5/month plans work)
3. **Install SMSTool APK**
4. **Configure**:
   - Enter backend URL: `ws://your-server:7777/ws`
   - Grant SMS permissions
   - Tap "Connect"
5. **Optional**:
   - Enable battery optimization bypass so the app stays running
   - Lock the device and place it somewhere safe (can be powered off, will still work when powered on if set to auto-restart)

---

## Development

### Project Structure

```
smstool/
├── backend/                    # Python FastAPI backend
│   ├── app/
│   │   ├── main.py
│   │   ├── config.py
│   │   ├── models/
│   │   ├── services/
│   │   │   ├── job_queue.py
│   │   │   └── ws_manager.py
│   │   └── api/v1/
│   │       └── endpoints/
│   │           ├── sms.py
│   │           ├── status.py
│   │           └── ws.py
│   ├── tests/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── requirements.txt
├── android/                    # Android app (Java/Gradle)
├── docs/
│   ├── PROTOCOL.md            # WebSocket message format
│   ├── API.md                 # REST API documentation
│   └── DEPLOYMENT.md          # Deployment guide
└── README.md
```

### Running Tests

```bash
cd backend
pytest tests/ -v              # All tests
pytest tests/test_api.py      # API tests only
pytest tests/test_ws_manager.py  # WebSocket tests only
```

### Making Changes

1. **Backend**: Edit `backend/app/` and restart the server
2. **Android**: Edit `android/app/src/main/` and rebuild APK in Android Studio
3. **API Spec**: Update `docs/PROTOCOL.md` and `docs/API.md`

---

## Troubleshooting

### Android app shows "Server offline"
- Check backend is running: `curl http://your-ip:7777/api/v1/health`
- Verify device can reach server (check firewall, ports)
- Ensure URL format is correct: `ws://` (not `http://`)

### SMS not sending
- Check device status: `curl http://your-ip:7777/api/v1/device/status`
- Verify device has active SIM with SMS credit
- Check Android app logs in the activity list

### Backend crashes
- Check logs: `docker compose logs smstool` or check console output
- Verify sufficient disk space for SQLite database
- Ensure port 7777 is not in use: `lsof -i :7777`

### Job queue too large
- In `docker-compose.yml`, adjust `JOB_QUEUE_MAX_SIZE`
- Default is 1000; reduce if running on low-memory device

---

## Security Considerations

⚠️ **Trust-Based Access** — This tool uses the backend URL as the only secret. It's designed for **private networks** (home, office VPN, corporate intranet).

For public internet use, add authentication:
- API key validation in `app/api/v1/endpoints/sms.py`
- TLS/HTTPS (use nginx reverse proxy with SSL cert)
- IP whitelisting in `docker-compose.yml` via network isolation

**SMS Abuse**: This tool can send any SMS from the device's number. Treat it like a shared resource.

---

## Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Add tests for new functionality
4. Run tests: `pytest tests/ -v`
5. Submit a pull request

---

## License

MIT License — See `LICENSE` file for details.

---

## Support

- 📖 [Full Protocol Documentation](docs/PROTOCOL.md)
- 📘 [REST API Reference](docs/API.md)
- 🚀 [Deployment Guide](docs/DEPLOYMENT.md)
- 💬 [GitHub Discussions](https://github.com/yourusername/smstool/discussions)
- 🐛 [Issue Tracker](https://github.com/yourusername/smstool/issues)

---

**Made with ❤️ for developers who need simple SMS gateway solutions.**
