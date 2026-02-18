# SMSTool - Java to Flutter Migration Guide

## Overview
This repository now contains **two versions** of the SMS Gateway application:
- **`android/`** - Original Java Android app (fully functional)
- **`flutter/`** - New Flutter port (under development)

## Quick Start

### Java Android App (Original)
```bash
cd android
./gradlew build
# Or build in Android Studio
```

### Flutter App (New)
```bash
cd flutter
flutter pub get
flutter build apk --debug
# Or run on device:
flutter run
```

## Architecture Comparison

| Aspect | Java | Flutter |
|--------|------|---------|
| **Framework** | Android Native (Java + Kotlin) | Flutter (Dart) |
| **Database** | Room ORM | sqflite |
| **State Management** | ViewModel + LiveData | Provider (ChangeNotifier) |
| **WebSocket** | OkHttp | web_socket_channel |
| **SMS Sending** | Direct SmsManager | Platform channel → SmsManager |
| **Foreground Service** | Android Service (START_STICKY) | flutter_foreground_task (pending) |
| **Permissions** | AndroidManifest + runtime (Java) | AndroidManifest + permission_handler |
| **UI** | Material Design (XML layouts) | Material Design (Dart widgets) |

## Feature Parity Status

### Core Features
- ✅ WebSocket connection to backend
- ✅ Persistent device ID (UUID)
- ✅ Server URL configuration
- ✅ SMS job queueing
- ✅ SMS sending via SmsManager
- ✅ Delivery report tracking
- ⏳ Background foreground service (Flutter: needs Phase 5)
- ⏳ Boot persistence (Flutter: needs Phase 5)
- ⏳ Automatic reconnection (Flutter: ready in code, needs Phase 5)
- ⏳ Retry logic (Flutter: ready in code, needs Phase 5)

### UI Features
- ✅ Single screen with status display
- ✅ Server URL input
- ✅ Device ID display + copy button
- ✅ Connect/Disconnect button
- ✅ Activity log viewer
- ✅ Error message display
- ⏳ Runtime permission prompts (Flutter: permission_handler ready, needs UI integration)

## Protocol Compatibility

Both apps use **identical WebSocket protocol**. Messages are compatible:

```json
// Device Info (sent on connect)
{
  "type": "device_info",
  "message_id": "<uuid>",
  "device_id": "<uuid>",
  "device_name": "<phone-model>",
  "android_version": 29,
  "app_version": "1.0.0",
  "connected_at": "<iso-8601>"
}

// SMS Job (received from server)
{
  "type": "sms_job",
  "job_id": "<uuid>",
  "to": "+1234567890",
  "body": "Message text",
  "max_retries": 3
}

// Status Update (sent to server)
{
  "type": "status_update",
  "message_id": "<uuid>",
  "job_id": "<uuid>",
  "status": "sent|delivered|failed_retrying|failed_permanent",
  "attempt": 1,
  "error_code": null,
  "error_message": null,
  "timestamp": "<iso-8601>"
}

// Ping (keep-alive from server)
{
  "type": "ping",
  "message_id": "<uuid>"
}

// Pong (keep-alive response)
{
  "type": "pong",
  "message_id": "<uuid>",
  "ping_message_id": "<original-uuid>",
  "timestamp": "<iso-8601>"
}
```

Both apps handle all these messages identically.

## Database Schema Compatibility

### Java (Room)
```
Table: sms_jobs
├── jobId (String, PK)
├── toNumber (String)
├── body (String)
├── status (String) [enum: queued/sending/sent/delivered/failed_retrying/failed_permanent]
├── attempts (Int)
├── maxRetries (Int)
├── createdAt (Long, ms since epoch)
├── updatedAt (Long, ms since epoch)
├── sentAt (Long?, ms since epoch)
├── deliveredAt (Long?, ms since epoch)
├── errorCode (Int?)
├── errorMessage (String?)
├── nextRetryAt (Long?, ms since epoch)
└── pendingReport (Int, 0|1)

Table: event_log
├── id (Int, PK, autoincrement)
├── timestamp (Long, ms since epoch)
├── level (String) [info/warn/error]
└── message (String)
```

### Flutter (sqflite)
**Identical schema** - both use the same field names and types.

## Shared Preferences Keys

| Key | Type | Usage |
|-----|------|-------|
| `device_id` | String | Persistent UUID for the device |
| `gateway_url` | String | User-configured WebSocket server URL |
| `service_running` | Boolean | Whether service should survive reboot |

Both apps use `com.smstool.gateway.prefs` file.

## Migration Path

### From Java to Flutter (in order)

1. **Phase 1-4 Complete** ✅
   - UI framework set up
   - Database layer fully implemented
   - Network layer ready
   - SMS platform channel implemented

2. **Phase 5 In Progress** ⏳
   - Integrate `flutter_foreground_task` for background service
   - Implement `GatewayTaskHandler` with WebSocket + job dispatch
   - Wire up MainViewModel to start/stop background service

3. **Phase 6 Optional** (Post-MVP)
   - Runtime permission dialogs
   - Better error UI
   - Job retry status visualization
   - Advanced logging/debugging UI

4. **Phase 7 Testing & Deployment** (Post-MVP)
   - Test against real backend server
   - Verify all edge cases
   - Performance profiling
   - Beta release

### Running Both Apps Concurrently

Since both use the same package name `com.smstool.gateway`, **only one can be installed at a time** on a device.

To test both:
- **Option A**: Rename Flutter package (e.g., `com.smstool.gateway.flutter`) in build.gradle
- **Option B**: Use two physical devices or emulators
- **Option C**: Use APK signing to install over (will replace Java version with Flutter version)

### Data Migration

If switching from Java app to Flutter app on the same device:
- Existing `sms_jobs` database will persist (same path, same schema)
- Existing SharedPreferences will be read (same file, same keys)
- Device ID will carry over automatically
- Server URL will carry over automatically
- Existing job queue will be available

**No data loss** occurs when switching between versions.

## Known Differences

### Java App
- Uses Room ORM (type-safe database)
- LiveData for reactive UI updates
- ViewModel with onSavedInstanceState
- Direct Activity + BroadcastReceiver lifecycle

### Flutter App
- Uses sqflite (simpler, less boilerplate)
- Provider (ChangeNotifier) for reactive UI
- Simpler state management (but manual lifecycle handling needed in Phase 5)
- Platform channels for native code interaction

## Debugging

### Java App
```bash
# View logs
adb logcat | grep "com.smstool.gateway"

# Run in debug mode
./gradlew installDebug
adb shell am start -D -N com.smstool.gateway/.MainActivity
```

### Flutter App
```bash
# View logs
flutter logs

# Run in debug mode
flutter run -v

# Build for profiling
flutter build apk --profile

# View Kotlin compilation errors
flutter build apk --debug 2>&1 | grep -A5 "error:"
```

## Server Backend Compatibility

**No changes needed to backend server!**

Both the Java and Flutter apps speak the same protocol. Your backend server can:
- Accept connections from either app version
- Send the same `sms_job` messages
- Receive the same `status_update` responses
- Handle device_info messages from both

The apps are **protocol-compatible** at the WebSocket level.

## Timeline

**Completed**: Phase 1-4 (~80% of core functionality)
- Project structure, data layer, network layer, SMS platform channel, basic UI

**In Progress**: Phase 5 (needs ~4-6 hours of focused work)
- Foreground service integration, task handler, lifecycle management

**Remaining**: Phase 6-7 (nice-to-have + testing)
- Permission dialogs, error handling refinements, testing

## Questions?

Refer to:
- `flutter/IMPLEMENTATION_STATUS.md` - Detailed implementation status
- `flutter/lib/` - Organized by layer (data, network, service, ui)
- `flutter/android/app/src/main/kotlin/` - Kotlin platform channel code
- `android/` - Original Java version (reference implementation)
