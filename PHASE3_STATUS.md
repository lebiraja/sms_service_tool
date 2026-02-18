# Phase 3 — Android Core Implementation — IN PROGRESS

## ✅ Completed (Skeleton + Core Services)

### Database Layer (100%)
- [x] **SmsJobEntity** — Room entity for job persistence
- [x] **EventLogEntity** — Room entity for activity log
- [x] **SmsJobDao** — Data access object with all queries
- [x] **EventLogDao** — Event log access
- [x] **AppDatabase** — Room database singleton with migration support

### Data Models & Preferences (100%)
- [x] **SmsJobStatus** — Enum with terminal/retryable checks
- [x] **PrefsManager** — SharedPreferences wrapper for settings
- [x] **SmsJobRepository** — High-level repository with async callbacks

### Network Layer (100%)
- [x] **WebSocketManager** — OkHttp WebSocket wrapper
- [x] **MessageParser** — JSON message construction & parsing utilities

### SMS Sending Layer (100%)
- [x] **SmsSender** — SmsManager wrapper with multi-part support
- [x] **SmsSentReceiver** — SMS_SENT broadcast handler with retry logic
- [x] **SmsDeliveredReceiver** — SMS delivery report handler

### Service Layer (100%)
- [x] **GatewayForegroundService** — Core daemon service (250+ lines)
  - WebSocket connection lifecycle
  - Reconnection with exponential backoff
  - Message routing (sms_job, ping, error)
  - Job dispatch to SmsSender
  - Pending report flushing
  - Foreground notification management
  - Boot/app-update recovery via BootReceiver

### Boot Recovery (100%)
- [x] **BootReceiver** — Auto-restart service on device boot

### Utilities (100%)
- [x] **NotificationHelper** — Notification channel creation & updates

### Configuration (100%)
- [x] **build.gradle** (app-level) — Dependencies + Android configuration
- [x] **build.gradle** (root) — Project build config
- [x] **settings.gradle** — Module configuration
- [x] **AndroidManifest.xml** — Permissions, activities, services, receivers

---

## ⏳ Remaining (Phase 3 UI + Testing)

### UI Layer (0%)
- [ ] **MainActivity** — Single-screen UI with:
  - Status indicator (MaterialChip, color-coded)
  - URL input field (TextInputEditText)
  - Connect/Disconnect button (MaterialButton)
  - Device ID display (with copy button)
  - Activity log RecyclerView
  - Permission request flow

- [ ] **MainViewModel** — State management:
  - ConnectionState (IDLE, CONNECTING, CONNECTED, ERROR)
  - EventLog LiveData
  - URL validation & saving
  - Service binding & lifecycle

- [ ] **Activity Log Adapter** — RecyclerView adapter for event log

### Testing (0%)
- [ ] **Room DAO Tests** — Database operation tests
- [ ] **Repository Tests** — Async callback tests
- [ ] **Service Integration Tests** — WebSocket lifecycle tests
- [ ] **UI Tests (Espresso)** — MainActivity interaction tests
- [ ] **E2E Tests** — Full flow (connect → send → deliver)

---

## Key Implementation Notes

### Database
- **Room ORM** with migrations for schema changes
- **LiveData** for UI updates on data changes
- **Callbacks** for async database operations (no coroutines)
- **Index** on `status` column for fast retry queries

### Service
- **Foreground Service** keeps connection alive during backgrounding
- **Exponential backoff** for reconnection: 5s → 10s → 20s → 40s → capped at 60s
- **Pending reports** stored locally if WebSocket down during status update
- **Handler-based retries** with `postDelayed()` for job retry scheduling

### Network
- **OkHttp 4.12** for robust WebSocket handling
- **Custom WebSocketListener** for extensibility
- **JSON via Gson** for message serialization
- **UUID** for unique job/message IDs

### SMS Sending
- **SmsManager** native Android SMS API
- **Multi-part** support for messages > 160 chars
- **Broadcast Receivers** for SENT and DELIVERY status
- **Retry logic** in SmsSentReceiver with exponential backoff

---

## File Structure

```
android/app/src/main/java/com/smstool/gateway/
├── MainActivity.java                          (TODO - Phase 3 UI)
├── data/
│   ├── db/
│   │   ├── AppDatabase.java                   ✓ (Complete)
│   │   ├── SmsJobEntity.java                  ✓ (Complete)
│   │   ├── SmsJobDao.java                     ✓ (Complete)
│   │   ├── EventLogEntity.java                ✓ (Complete)
│   │   └── EventLogDao.java                   ✓ (Complete)
│   ├── model/
│   │   └── SmsJobStatus.java                  ✓ (Complete)
│   ├── prefs/
│   │   └── PrefsManager.java                  ✓ (Complete)
│   └── repository/
│       └── SmsJobRepository.java              ✓ (Complete)
├── network/
│   ├── WebSocketManager.java                  ✓ (Complete)
│   └── MessageParser.java                     ✓ (Complete)
├── service/
│   ├── GatewayForegroundService.java          ✓ (Complete)
│   └── SmsSender.java                         ✓ (Complete)
├── receiver/
│   ├── BootReceiver.java                      ✓ (Complete)
│   ├── SmsSentReceiver.java                   ✓ (Complete)
│   └── SmsDeliveredReceiver.java              ✓ (Complete)
├── ui/viewmodel/
│   └── MainViewModel.java                     (TODO)
├── util/
│   └── NotificationHelper.java                ✓ (Complete)
└── res/
    ├── layout/
    │   └── activity_main.xml                  (TODO)
    └── values/
        ├── strings.xml                        (TODO)
        ├── colors.xml                         (TODO)
        └── dimens.xml                         (TODO)
```

---

## Next Steps

1. **Create MainViewModel** — LiveData for connection state + event log
2. **Build MainActivity UI** — Layout XML + view binding
3. **Implement Runtime Permissions** — REQUEST_SEND_SMS at runtime
4. **Connect Service Binding** — MainActivity binds to GatewayForegroundService
5. **Wire UI Actions** — Connect button → service start/stop
6. **Add Tests** — Unit + integration + E2E tests
7. **Test End-to-End** — Backend + Device + SMS delivery

---

## Architecture Diagram

```
┌────────────────────────────────────┐
│      MainActivity (UI)              │
│  ─ Status chip                      │
│  ─ URL input + Connect button       │
│  ─ Activity log                     │
└─────────┬──────────────────────────┘
          │
          ├─── Binding ───┐
          │               │
    ┌─────▼──────────────────────────┐
    │  MainViewModel                  │
    │  ─ ConnectionState LiveData      │
    │  ─ EventLog LiveData             │
    │  ─ URL validation & save         │
    └─────┬──────────────────────────┘
          │
          │
    ┌─────▼──────────────────────────┐
    │  GatewayForegroundService       │
    │  ─ WebSocket connection         │
    │  ─ Reconnection logic           │
    │  ─ Job dispatch to SmsSender    │
    │  ─ Persistent notification      │
    └─────┬──────────────────────────┘
          │
          ├─────────────────────────┬────────────────────┐
          │                         │                    │
    ┌─────▼────────┐      ┌────────▼────────┐     ┌─────▼──────────────┐
    │ SmsSender    │      │ SmsJobRepository│     │ PrefsManager       │
    │ ─ SmsManager │      │ ─ Room DB       │     │ ─ SharedPrefs      │
    │ ─ Multi-part │      │ ─ Async ops     │     │ ─ URL + Device ID  │
    └─────┬────────┘      └────────┬────────┘     └─────────────────────┘
          │                        │
    ┌─────▼────────────────┬──────▼────────────┐
    │                      │                   │
    │ Broadcast Receivers  │  Room Database    │
    │                      │                   │
    │ SmsSentReceiver      │  SmsJobEntity     │
    │ ├ SENT handler      │  EventLogEntity   │
    │ └ Retry + backoff   │                   │
    │                      │  DAOs:            │
    │ SmsDeliveredReceiver │  ├ SmsJobDao     │
    │ └ DELIVERY handler   │  └ EventLogDao   │
    │                      │                   │
    │ BootReceiver         │                   │
    │ └ Auto-restart       │                   │
    └──────────────────────┴──────────────────┘
          │
    ┌─────▼─────────────────┐
    │  Android SMS System    │
    │  (SmsManager API)      │
    │  ─ Send SMS            │
    │  ─ Multi-part support  │
    └─────┬─────────────────┘
          │
    ┌─────▼─────────────────┐
    │  Device SIM           │
    │  ─ Mobile Network     │
    │  ─ SMS to Recipient   │
    └───────────────────────┘
```

---

## Statistics

- **Java files created**: 17
- **Lines of code**: ~2,000 (excluding tests)
- **Database tables**: 2 (sms_jobs, event_log)
- **Permissions required**: 8
- **Broadcast receivers**: 3
- **Services**: 1 (foreground)

---

**Phase 3 Progress: ~70% complete**

Remaining: MainActivity UI + Binding + Runtime Permissions + Tests
