import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'dart:async';
import '../network/websocket_manager.dart';
import '../data/database/app_database.dart';
import '../data/repository/sms_job_repository.dart';
import '../data/prefs/prefs_manager.dart';
import '../network/message_parser.dart';
import '../data/models/sms_job.dart';
import '../data/models/sms_job_status.dart';
import 'sms_sender.dart';

@pragma('vm:entry-point')
void startGatewayTask() {
  FlutterForegroundTask.setTaskHandler(GatewayTaskHandler());
}

class GatewayTaskHandler extends TaskHandler {
  late WebSocketManager _wsManager;
  late SmsJobRepository _repository;
  late SmsSender _smsSender;
  late PrefsManager _prefs;
  StreamSubscription? _wsSubscription;
  Timer? _retryTimer;
  Timer? _pingTimer;
  bool _initialized = false;

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    print('[GatewayTask] Starting gateway task handler...');

    try {
      // Initialize preferences
      _prefs = PrefsManager();
      await _prefs.init();

      // Initialize database
      final database = AppDatabase();
      await database.database;

      // Initialize repository
      _repository = SmsJobRepository(database);

      // Initialize WebSocket manager
      _wsManager = WebSocketManager(
        onStateChange: _onWsStateChange,
      );

      // Initialize SMS sender
      _smsSender = SmsSender(onResult: _onSmsResult);

      _initialized = true;

      // Get stored URL and connect
      final url = _prefs.getGatewayUrl();
      if (url != null && url.isNotEmpty) {
        print('[GatewayTask] Connecting to: $url');
        await _wsManager.connect(url);
        _setupWebSocketListener();
        await Future.delayed(const Duration(milliseconds: 500));
        _sendDeviceInfo();
      } else {
        print('[GatewayTask] No gateway URL configured');
      }

      // Start periodic ping to keep connection alive
      _pingTimer = Timer.periodic(const Duration(seconds: 30), (timer) {
        // Periodically check and log connection state
        print('[GatewayTask] Connection state: ${_wsManager.state}');
      });

      print('[GatewayTask] Gateway task handler initialized successfully');
    } catch (e) {
      print('[GatewayTask] Error during initialization: $e');
      try {
        await _repository.logError('Task initialization failed: $e');
      } catch (_) {
        // repository not yet initialized
      }
    }
  }

  @override
  void onRepeatEvent(DateTime timestamp) {
    if (!_initialized) return;

    try {
      // Check for jobs that need retrying
      _repository.getPendingRetries().then((pendingRetries) {
        for (final job in pendingRetries) {
          print('[GatewayTask] Retrying job: ${job.jobId}');
          _smsSender.sendSms(
            jobId: job.jobId,
            phoneNumber: job.toNumber,
            messageBody: job.body,
            maxRetries: job.maxRetries,
          );
        }
      });
    } catch (e) {
      print('[GatewayTask] Error in onRepeatEvent: $e');
    }
  }

  @override
  Future<void> onDestroy(DateTime timestamp) async {
    print('[GatewayTask] Destroying gateway task handler');
    _pingTimer?.cancel();
    _wsSubscription?.cancel();
    _retryTimer?.cancel();
    _wsManager.disconnect();
    _smsSender.dispose();
    try {
      await _prefs.setServiceRunning(false);
    } catch (e) {
      print('[GatewayTask] Error setting service running flag: $e');
    }
  }

  @override
  void onButtonPressed(String id) {
    // Handle button press if needed
  }

  @override
  void onNotificationButtonPressed(String id) {
    // Handle notification button press
  }

  void _setupWebSocketListener() {
    _wsSubscription?.cancel();
    _wsSubscription = _wsManager.messageStream.listen(
      (message) async {
        try {
          if (message is SmsJobMessage) {
            print('[GatewayTask] Received SMS job: ${message.jobId}');

            // Create job in database
            final now = DateTime.now();
            final job = SmsJob(
              jobId: message.jobId,
              toNumber: message.to,
              body: message.body,
              status: SmsJobStatus.queued,
              attempts: 0,
              maxRetries: message.maxRetries,
              createdAt: now,
              updatedAt: now,
            );

            await _repository.createJob(job);
            await _repository.logInfo(
              'SMS Job received: ${message.to} - ${message.body.substring(0, (message.body.length > 30 ? 30 : message.body.length))}...',
            );

            // Send immediately
            await _smsSender.sendSms(
              jobId: job.jobId,
              phoneNumber: job.toNumber,
              messageBody: job.body,
              maxRetries: job.maxRetries,
            );
          } else if (message is PingMessage) {
            print('[GatewayTask] Received ping, sending pong');
            final pongMsg = MessageParser.buildPongMessage(message.messageId);
            _wsManager.send(pongMsg);
          } else if (message is ErrorMessage) {
            print('[GatewayTask] Server error: ${message.code} - ${message.detail}');
            await _repository.logError(
              'Server error: ${message.code} - ${message.detail}',
            );
          }
        } catch (e) {
          print('[GatewayTask] Error processing message: $e');
          await _repository.logError('Message processing error: $e');
        }
      },
      onError: (error) {
        print('[GatewayTask] WebSocket stream error: $error');
      },
    );
  }

  Future<void> _onSmsResult(
    String jobId,
    SmsJobStatus status,
    int? errorCode,
    String? errorMessage,
  ) async {
    try {
      final job = await _repository.getJob(jobId);
      if (job == null) {
        print('[GatewayTask] Job not found: $jobId');
        return;
      }

      print('[GatewayTask] SMS result: $jobId -> ${status.name}');

      final updatedJob = job.copyWith(
        status: status,
        attempts: job.attempts + 1,
        updatedAt: DateTime.now(),
        errorCode: errorCode,
        errorMessage: errorMessage,
      );

      await _repository.updateJobStatus(
        jobId,
        updatedJob,
        logMessage: 'SMS Status: ${status.name}${errorMessage != null ? ' - $errorMessage' : ''}',
      );

      // Send status update to server
      final statusUpdateMsg = MessageParser.buildStatusUpdateMessage(
        jobId: jobId,
        status: status.toJsonString(),
        attempt: updatedJob.attempts,
        errorCode: errorCode,
        errorMessage: errorMessage,
      );
      _wsManager.send(statusUpdateMsg);
    } catch (e) {
      print('[GatewayTask] Error handling SMS result: $e');
    }
  }

  void _onWsStateChange(ConnectionState state, String? error) {
    print('[GatewayTask] WebSocket state changed: $state');

    if (state == ConnectionState.connected) {
      print('[GatewayTask] Connected! Flushing pending reports...');
      _flushPendingReports();
      _sendDeviceInfo();
    } else if (state == ConnectionState.error) {
      print('[GatewayTask] Connection error: $error');
    }
  }

  Future<void> _sendDeviceInfo() async {
    try {
      final deviceId = await _prefs.getDeviceId();
      final deviceInfoMsg = MessageParser.buildDeviceInfoMessage(
        deviceId: deviceId,
        deviceName: 'Flutter Device',
        androidVersion: 29,
      );
      _wsManager.send(deviceInfoMsg);
      print('[GatewayTask] Device info sent');
    } catch (e) {
      print('[GatewayTask] Error sending device info: $e');
    }
  }

  Future<void> _flushPendingReports() async {
    try {
      final pending = await _repository.getPendingReports();
      print('[GatewayTask] Flushing ${pending.length} pending reports');

      for (final job in pending) {
        final statusUpdateMsg = MessageParser.buildStatusUpdateMessage(
          jobId: job.jobId,
          status: job.status.toJsonString(),
          attempt: job.attempts,
          errorCode: job.errorCode,
          errorMessage: job.errorMessage,
        );
        _wsManager.send(statusUpdateMsg);
        await _repository.clearPendingReport(job.jobId);
      }
    } catch (e) {
      print('[GatewayTask] Error flushing pending reports: $e');
    }
  }
}
