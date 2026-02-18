# Flutter SMS Gateway - Implementation Status

## Completed (Phases 1-4)

### Phase 1: Project Scaffold ✅
- Created Flutter project in `flutter/` directory
- Configured package name: `com.smstool.gateway`
- Set app name: "SMS Gateway"
- Added all required dependencies in pubspec.yaml
- Created directory structure for Dart code

### Phase 2: Data Layer ✅
- **Database**: SQLite via sqflite with Room-equivalent schema
  - `sms_jobs` table: job queue with all required fields
  - `event_log` table: activity log with 500-entry cap
  - `AppDatabase` singleton with connection pooling

- **Models**: Type-safe models
  - `SmsJob` with full mapping to/from database
  - `SmsJobStatus` enum (queued/sending/sent/delivered/failed_retrying/failed_permanent)
  - `EventLogEntry` with level enum (info/warn/error)

- **DAOs**: Full CRUD operations
  - `SmsJobDao`: insert, update status, get, get pending retries, manage pending reports
  - `EventLogDao`: insert with auto-cleanup, get latest N entries

- **Preferences**: SharedPreferences wrapper
  - Device ID (UUID, generated once, persisted)
  - Gateway URL (user-configurable)
  - Service running flag (for boot persistence)

- **Repository**: Convenience layer combining DB + logging
  - `SmsJobRepository`: high-level job and event management

### Phase 3: Network Layer ✅
- **WebSocketManager**: OkHttp equivalent via web_socket_channel
  - Connection state machine (idle/connecting/connected/error)
  - Automatic reconnection with exponential backoff (5s → 60s max)
  - Message stream exposed as Stream<ServerMessage>
  - URL normalization (adds ws://, appends /ws)

- **MessageParser**: JSON serialization for all 6 message types
  - `SmsJobMessage`: parse incoming SMS job commands
  - `PingMessage`: keep-alive from server
  - `ErrorMessage`: error notifications
  - Builders for: device_info, status_update, pong (to send to server)

### Phase 4: SMS Platform Channel ✅
- **Kotlin SmsPlatformChannel**:
  - MethodChannel: `sendSms(jobId, to, body, maxRetries)` → Android SmsManager
  - Handles multi-part messages via `divideMessage()`
  - Registers PendingIntents for SMS_SENT and SMS_DELIVERED results
  - EventChannel: `smsDeliveryEvents` broadcasts `{jobId, status, errorCode, errorMessage}`

- **SmsSentReceiver** (Kotlin BroadcastReceiver):
  - Receives SMS_SENT PendingIntent results
  - Maps Android error codes to gateway statuses:
    - RESULT_OK → "sent"
    - RESULT_ERROR_NO_SERVICE → "failed_retrying"
    - RESULT_ERROR_NULL_PDU → "failed_permanent"
    - Others → "failed_retrying"
  - Broadcasts result via EventChannel

- **SmsDeliveredReceiver** (Kotlin BroadcastReceiver):
  - Receives SMS_DELIVERED PendingIntent results
  - On success: broadcasts "delivered"
  - Handles missing delivery reports gracefully

- **SmsSender** (Dart service):
  - Calls platform channel `sendSms()` method
  - Listens on EventChannel for delivery results
  - Callback notifies on SMS status changes

### Phase 6: UI ✅
- **MainViewModel** (ChangeNotifier):
  - Connection state (idle/connecting/connected/error)
  - Server URL management with normalization
  - Device ID display and copy
  - Event log streaming (last 50 entries)
  - Connect/disconnect commands

- **MainScreen**:
  - Blue AppBar with "SMS Gateway" title
  - Status chip (color-coded, state-dependent text)
  - Server URL input (disabled while connecting)
  - Device ID row with copy button
  - Connect/Disconnect/Retry button (state-dependent)
  - Error message display
  - Activity log viewer (300dp ListView of latest events)

- **Widgets**:
  - `StatusChip`: color-coded connection state indicator
  - `EventLogList`: ListView.builder for activity log
  - `EventLogItem`: timestamp + icon + message row

---

## Pending (Phase 5)

### Phase 5: Foreground Service Integration ❌
**Note**: This phase requires careful implementation with `flutter_foreground_task` plugin.

**What needs to be done**:

1. **Add flutter_foreground_task to pubspec.yaml** (not yet added)
   ```yaml
   flutter_foreground_task: ^8.x
   ```

2. **Create gateway_task_handler.dart** (currently has commented-out implementation)
   - Implement `@pragma('vm:entry-point')` task handler
   - Runs in separate isolate managed by flutter_foreground_task
   - Responsibilities:
     - Initialize DB, prefs, WebSocketManager, SmsSender in background isolate
     - Connect to gateway WebSocket on app start
     - Listen for `sms_job` messages and dispatch to SmsSender
     - Respond to `ping` with `pong`
     - On reconnect: flush pending reports and retry failed jobs
     - Main ↔ background isolate communication via `sendDataToTask()`/`sendDataToMain()`

3. **Configure AndroidManifest.xml**
   - Declare flutter_foreground_task's `ForegroundService` component
   - Declare boot receiver: `com.pravera.flutter_foreground_task.receiver.ForegroundTaskBootReceiver`
   - Add BOOT_COMPLETED and MY_PACKAGE_REPLACED intent filters

4. **Integrate with MainViewModel**
   - Call `FlutterForegroundTask.startService()` on connect
   - Call `FlutterForegroundTask.stopService()` on disconnect
   - Listen for status updates from background task via receive port

5. **Testing**
   - Verify service persists when app is killed
   - Verify service auto-restarts after device reboot
   - Verify pending jobs are retried on reconnect
   - Verify SMS jobs received from server are sent

---

## Known Limitations & Next Steps

### Current State
- **UI is functional**: Can connect/disconnect, configure server, view activity log
- **SMS sending architecture is ready**: Platform channel implemented, just needs to be called from task handler
- **Database is ready**: Full schema and DAOs implemented
- **WebSocket connection is ready**: Can connect and parse messages

### What's Not Yet Working
- **Background service**: Not running yet; needs flutter_foreground_task integration
- **SMS sending**: Platform channel exists but isn't being invoked (requires task handler)
- **Boot persistence**: Will work once foreground task is implemented
- **Real server integration**: Needs to be tested against actual backend

### Recommended Next Steps
1. Add `flutter_foreground_task` package
2. Implement the commented-out `GatewayTaskHandler` in gateway_task_handler.dart
3. Update AndroidManifest.xml with foreground service declarations
4. Wire up MainViewModel to start/stop the background task
5. Test against a test WebSocket server
6. Add runtime permission handling for SMS (permission_handler is already added)

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────┐
│ Flutter UI Layer                                        │
│ (MainScreen + MainViewModel)                            │
└──────────────────┬──────────────────────────────────────┘
                   │ ChangeNotifier / Provider
                   ↓
┌─────────────────────────────────────────────────────────┐
│ Main Isolate                                            │
│ ├─ UI (MainScreen)                                      │
│ ├─ ViewModel (MainViewModel)                            │
│ ├─ Preferences (PrefsManager)                           │
│ └─ Control messages ↔ Background Task                   │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ↓ start/stop service commands
┌──────────────────────────────────────────────────────────┐
│ Android Foreground Service (flutter_foreground_task)   │
│                                                         │
│ ┌─ Background Isolate                                  │
│ │  ├─ GatewayTaskHandler                               │
│ │  ├─ WebSocketManager (persistent connection)         │
│ │  ├─ SmsJobRepository (DB access)                     │
│ │  ├─ SmsSender (platform channel calls)               │
│ │  └─ Retry timer for failed_retrying jobs             │
│ │                                                       │
│ └─ Data → Platform Channel → Android SmsManager        │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ↓
┌──────────────────────────────────────────────────────────┐
│ Kotlin Platform Channel                                 │
│ ├─ SmsPlatformChannel (MethodChannel handler)           │
│ ├─ SmsSentReceiver (SMS_SENT PendingIntent)             │
│ ├─ SmsDeliveredReceiver (SMS_DELIVERED PendingIntent)  │
│ └─ Results → EventChannel → Dart SmsSender              │
└──────────────────────────────────────────────────────────┘
```

---

## File Structure

```
flutter/
├── lib/
│   ├── main.dart                              # App entry point
│   ├── app.dart                               # MaterialApp + Provider setup
│   ├── data/
│   │   ├── database/
│   │   │   ├── app_database.dart              # SQLite singleton
│   │   │   ├── sms_job_dao.dart               # SMS job CRUD
│   │   │   └── event_log_dao.dart             # Event log operations
│   │   ├── models/
│   │   │   ├── sms_job.dart                   # SmsJob model
│   │   │   ├── sms_job_status.dart            # Status enum
│   │   │   └── event_log_entry.dart           # EventLog model
│   │   ├── prefs/
│   │   │   └── prefs_manager.dart             # SharedPreferences wrapper
│   │   └── repository/
│   │       └── sms_job_repository.dart        # High-level repository
│   ├── network/
│   │   ├── websocket_manager.dart             # WebSocket connection
│   │   └── message_parser.dart                # JSON message parsing
│   ├── service/
│   │   ├── gateway_task_handler.dart          # [TODO] Background task
│   │   └── sms_sender.dart                    # Platform channel caller
│   └── ui/
│       ├── screens/
│       │   └── main_screen.dart               # Single screen
│       ├── widgets/
│       │   ├── status_chip.dart               # Status display
│       │   ├── event_log_list.dart            # Log list view
│       │   └── event_log_item.dart            # Log row item
│       └── viewmodels/
│           └── main_viewmodel.dart            # ChangeNotifier state
├── android/
│   └── app/src/main/
│       ├── kotlin/com/smstool/gateway/
│       │   ├── MainActivity.kt                # Flutter entry + channel setup
│       │   ├── SmsPlatformChannel.kt          # MethodChannel + EventChannel
│       │   ├── SmsSentReceiver.kt             # SMS_SENT receiver
│       │   └── SmsDeliveredReceiver.kt        # SMS_DELIVERED receiver
│       └── AndroidManifest.xml                # Permissions + declarations
└── pubspec.yaml                               # Dependencies
```

---

## Testing Checklist

- [ ] Flutter build succeeds (✅ done)
- [ ] App launches on device
- [ ] Can enter server URL and tap Connect
- [ ] Network errors are shown in UI
- [ ] Device ID can be copied to clipboard
- [ ] Activity log displays connection state changes
- [ ] Background service persists when app is killed
- [ ] Background service auto-restarts after reboot
- [ ] SMS jobs are received from server and sent
- [ ] SMS delivery reports are tracked
- [ ] Failed jobs are retried with correct backoff
- [ ] Pending reports are flushed on reconnect
