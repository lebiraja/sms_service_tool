# SMSTool — Complete Setup & Testing Guide

## 📋 Prerequisites

### For Backend:
- Python 3.12+
- Docker & Docker Compose (optional, but recommended)
- curl or Postman for API testing

### For Android:
- Android Studio
- Android SDK 34+
- JDK 17+
- A physical Android 10+ device OR Android emulator

---

## 🚀 Part 1: Backend Setup & Testing

### Option A: Docker (Recommended - Fastest)

**1. Start the backend with Docker:**
```bash
cd /home/lebi/projects/smstool/backend
docker compose up -d
```

**Output:** Backend runs on `http://localhost:7777`

**2. Verify it's running:**
```bash
curl http://localhost:7777/api/v1/health
```

**Expected output:**
```json
{
  "status": "ok",
  "version": "1.0.0",
  "device_connected": false
}
```

**3. Stop the backend:**
```bash
cd /home/lebi/projects/smstool/backend
docker compose down
```

---

### Option B: Local Python (Manual Setup)

**1. Create Python virtual environment:**
```bash
cd /home/lebi/projects/smstool
python3 -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
```

**2. Install dependencies:**
```bash
cd backend
pip install -r requirements.txt
```

**3. Run the backend:**
```bash
uvicorn app.main:app --host 0.0.0.0 --port 7777
```

**Output:**
```
INFO:     Uvicorn running on http://0.0.0.0:7777
INFO:     Application startup complete
```

**4. Stop the backend:**
Press `Ctrl+C` in the terminal

---

## 🧪 Part 2: Test Backend API

### Keep backend running in one terminal, test in another

**1. Test health check:**
```bash
curl http://localhost:7777/api/v1/health
```

**Expected response:**
```json
{
  "status": "ok",
  "version": "1.0.0",
  "device_connected": false
}
```

**2. Test device status (no device connected yet):**
```bash
curl http://localhost:7777/api/v1/device/status
```

**Expected response:**
```json
{
  "connected": false,
  "device_id": null,
  "device_name": null,
  "android_version": null,
  "app_version": null,
  "sim_operator": null,
  "sim_country": null,
  "connected_at": null,
  "last_ping_at": null
}
```

**3. Try to send SMS (will fail - no device connected):**
```bash
curl -X POST http://localhost:7777/api/v1/sms/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": "+15551234567",
    "body": "Hello from SMSTool!",
    "max_retries": 3
  }'
```

**Expected response (503 - no device):**
```json
{
  "detail": {
    "error": "DEVICE_NOT_CONNECTED",
    "detail": "No Android device is currently connected to the gateway"
  }
}
```

**4. Run backend unit tests:**
```bash
cd /home/lebi/projects/smstool/backend
source ../venv/bin/activate  # Activate venv if not already
python -m pytest tests/ -v
```

**Expected output:**
```
============================= test session starts ==============================
...
======================= 37 passed in 0.61s ========================
```

---

## 📱 Part 3: Build & Install Android App

### Step 1: Open project in Android Studio

```bash
# Navigate to the Android project
cd /home/lebi/projects/smstool/android

# Open in Android Studio
open -a "Android Studio" .  # On macOS
# On Windows: Start Android Studio manually and open the project
# On Linux: studio.sh . (if android-studio is in PATH)
```

### Step 2: Build the APK

**In Android Studio:**
1. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for the build to complete
3. Look for the success message at the bottom

**OR from command line:**
```bash
cd /home/lebi/projects/smstool/android

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing key)
./gradlew assembleRelease

# Output will be in: app/build/outputs/apk/
```

### Step 3: Install on device

**Option A: Using Android Studio**
1. Connect Android device via USB
2. Enable USB debugging on device
3. In Android Studio: **Run** → **Run 'app'**
4. Select your device and click OK

**Option B: Using adb from command line**
```bash
# List connected devices
adb devices

# Install the APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or if you built release:
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🔌 Part 4: Connect Android App to Backend

### On your Android device (or emulator):

**1. Open the SMS Gateway app**
- You'll see a screen with "Not connected" status

**2. Enter the backend URL**
- For **local network**: `192.168.1.XXX:7777` (replace with your computer's IP)
- For **emulator to local backend**: `10.0.2.2:7777` (special emulator IP for localhost)
- For **public server**: `your-server.com:7777`

**3. Tap "Connect" button**
- Status should change to "Connecting..." (yellow)
- Then "Connected" (green) when successful
- Activity log will show connection events

**4. Verify connection on backend**
```bash
curl http://localhost:7777/api/v1/device/status
```

**Expected response (now showing connected device):**
```json
{
  "connected": true,
  "device_id": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "device_name": "Pixel 7 Pro",
  "android_version": "14",
  "app_version": "1.0.0",
  "sim_operator": "T-Mobile",
  "sim_country": "us",
  "connected_at": "2026-02-17T21:00:00Z",
  "last_ping_at": "2026-02-17T21:05:30Z"
}
```

---

## 📤 Part 5: Send Test SMS

### Now with device connected, send an SMS!

**1. Send SMS via REST API:**
```bash
curl -X POST http://localhost:7777/api/v1/sms/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": "+15551234567",
    "body": "Hello from SMSTool!",
    "max_retries": 3
  }'
```

**Expected response (202 Accepted):**
```json
{
  "job_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "queued",
  "created_at": "2026-02-17T21:00:00Z"
}
```

**2. On the Android app:**
- Activity log should show: "Received job to +15551234567"
- Status changes: queued → sending → sent
- Check the log for emoji indicators:
  - ✓ = Success
  - ⚠ = Warning
  - ✕ = Error

**3. Check SMS status:**
```bash
# Replace with the job_id from above
curl http://localhost:7777/api/v1/sms/jobs/f47ac10b-58cc-4372-a567-0e02b2c3d479
```

**Expected response:**
```json
{
  "job_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "to": "+15551234567",
  "body": "Hello from SMSTool!",
  "status": "sent",
  "attempts": 1,
  "max_retries": 3,
  "created_at": "2026-02-17T21:00:00Z",
  "sent_at": "2026-02-17T21:00:05Z",
  "delivered_at": null,
  "error_code": null,
  "error_message": null
}
```

**4. List all jobs:**
```bash
curl http://localhost:7777/api/v1/sms/jobs
```

**Expected response:**
```json
{
  "total": 1,
  "limit": 50,
  "offset": 0,
  "jobs": [
    {
      "job_id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "to": "+15551234567",
      "body": "Hello from SMSTool!",
      "status": "sent",
      "attempts": 1,
      "max_retries": 3,
      "created_at": "2026-02-17T21:00:00Z",
      "sent_at": "2026-02-17T21:00:05Z",
      "delivered_at": null,
      "error_code": null,
      "error_message": null
    }
  ]
}
```

---

## 🔍 Part 6: Advanced Testing

### Test with multiple SMS jobs

```bash
# Send 5 SMS messages
for i in {1..5}; do
  curl -X POST http://localhost:7777/api/v1/sms/send \
    -H "Content-Type: application/json" \
    -d "{
      \"to\": \"+155512345${i}0\",
      \"body\": \"Test message $i\",
      \"max_retries\": 3
    }"
  echo ""
  sleep 1  # Wait 1 second between sends
done
```

### Test filtering jobs by status

```bash
# Get only sent jobs
curl "http://localhost:7777/api/v1/sms/jobs?status=sent&limit=10"

# Get only failed jobs
curl "http://localhost:7777/api/v1/sms/jobs?status=failed_permanent&limit=10"

# Paginate results (limit 5 per page)
curl "http://localhost:7777/api/v1/sms/jobs?limit=5&offset=0"  # Page 1
curl "http://localhost:7777/api/v1/sms/jobs?limit=5&offset=5"  # Page 2
```

### Test with Postman (optional)

1. **Import collection:**
   ```bash
   # Create a Postman collection file
   cat > /tmp/smstool-postman.json << 'EOF'
   {
     "info": {"name": "SMSTool", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
     "item": [
       {
         "name": "Health Check",
         "request": {
           "method": "GET",
           "url": "http://localhost:7777/api/v1/health"
         }
       },
       {
         "name": "Send SMS",
         "request": {
           "method": "POST",
           "header": [{"key": "Content-Type", "value": "application/json"}],
           "body": {"mode": "raw", "raw": "{\"to\": \"+15551234567\", \"body\": \"Test\"}"},
           "url": "http://localhost:7777/api/v1/sms/send"
         }
       }
     ]
   }
   EOF
   ```

2. In Postman: **File** → **Import** → Select the file

---

## 🧪 Part 7: Full End-to-End Test Script

Save this as `test-smstool.sh`:

```bash
#!/bin/bash

echo "=========================================="
echo "SMSTool — End-to-End Test"
echo "=========================================="
echo ""

BACKEND_URL="http://localhost:7777"

# 1. Health check
echo "1️⃣  Testing health check..."
curl -s "$BACKEND_URL/api/v1/health" | jq .
echo ""

# 2. Device status (before connection)
echo "2️⃣  Checking device status (should be disconnected)..."
curl -s "$BACKEND_URL/api/v1/device/status" | jq .
echo ""

# 3. Try to send SMS (should fail - no device)
echo "3️⃣  Attempting to send SMS (should fail - no device)..."
curl -s -X POST "$BACKEND_URL/api/v1/sms/send" \
  -H "Content-Type: application/json" \
  -d '{"to":"+15551234567","body":"Test"}' | jq .
echo ""

echo "⏳ Waiting for Android device to connect... (30 seconds)"
echo "   → Open the app on your Android device"
echo "   → Enter backend URL and tap Connect"
sleep 30
echo ""

# 4. Check device status (should be connected)
echo "4️⃣  Checking device status (should be CONNECTED)..."
curl -s "$BACKEND_URL/api/v1/device/status" | jq .
echo ""

# 5. Send SMS (should succeed now)
echo "5️⃣  Sending test SMS..."
RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/sms/send" \
  -H "Content-Type: application/json" \
  -d '{"to":"+15551234567","body":"Hello from SMSTool!"}')
echo "$RESPONSE" | jq .
JOB_ID=$(echo "$RESPONSE" | jq -r '.job_id')
echo ""

# 6. Check job status
echo "6️⃣  Checking job status..."
sleep 2
curl -s "$BACKEND_URL/api/v1/sms/jobs/$JOB_ID" | jq .
echo ""

# 7. List all jobs
echo "7️⃣  Listing all jobs..."
curl -s "$BACKEND_URL/api/v1/sms/jobs" | jq .
echo ""

echo "✅ All tests completed!"
```

**Run it:**
```bash
chmod +x test-smstool.sh
./test-smstool.sh
```

---

## 🐛 Troubleshooting

### Backend won't start
```bash
# Check if port 7777 is in use
lsof -i :7777

# Kill the process if needed
kill -9 <PID>

# Try a different port
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

### Can't connect from Android emulator to backend
```bash
# Emulator uses special IP for localhost
# Use: 10.0.2.2:7777 instead of localhost:7777

# Verify with:
adb shell ping 10.0.2.2
```

### Can't connect from Android device on same network
```bash
# Get your computer's IP
ifconfig  # macOS/Linux
ipconfig  # Windows

# Use that IP instead of localhost
# Example: 192.168.1.100:7777
```

### Tests fail
```bash
# Make sure to activate venv first
source venv/bin/activate

# Then run tests
cd backend
python -m pytest tests/ -v --tb=short
```

### App crashes on startup
```bash
# Check Android logs
adb logcat | grep "SMSTool"

# Or in Android Studio: View → Tool Windows → Logcat
```

---

## 📊 Complete Test Checklist

- [ ] Backend starts successfully (`docker compose up -d` or `uvicorn app.main:app`)
- [ ] Health check returns OK (`curl http://localhost:7777/api/v1/health`)
- [ ] Device status shows disconnected (before app connects)
- [ ] Backend tests pass (`pytest tests/ -v`)
- [ ] Android app installs without errors
- [ ] Android app opens and shows "Not connected"
- [ ] App connects to backend successfully
- [ ] Device status shows connected with device info
- [ ] Can send SMS via REST API
- [ ] Activity log shows job status changes
- [ ] Job status endpoint returns correct status
- [ ] List jobs endpoint returns all jobs
- [ ] All tests pass (37/37)

---

## 🎯 Expected Final State

**Backend Terminal:**
```
INFO:     Uvicorn running on http://0.0.0.0:7777
INFO:     Application startup complete
```

**Android Device:**
- Green status chip saying "Connected"
- Device info showing: "Pixel 7 Pro · T-Mobile"
- Activity log showing: "✓ Connected to server"

**After sending SMS:**
- Activity log shows: "Received job to +15551234567"
- Status: queued → sending → sent
- Job endpoint returns "status": "sent"

---

## 🚀 Next Steps

1. **Test with real SMS** (if you have a SIM card)
   - Enter a real phone number
   - Verify SMS is actually sent

2. **Test reconnection**
   - Stop backend: `docker compose down`
   - Watch app show "Reconnecting..." with attempt counter
   - Start backend: `docker compose up -d`
   - App reconnects automatically

3. **Test boot recovery**
   - Reboot device while service is running
   - App restarts service automatically after boot

4. **Load testing**
   - Send 100+ SMS jobs
   - Monitor queue and status updates

---

**You're all set! Happy SMS sending! 🎉**
