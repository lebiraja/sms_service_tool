import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'dart:async';
import 'dart:developer' as developer;
import '../network/http_client.dart';
import '../data/database/app_database.dart';
import '../data/repository/sms_job_repository.dart';
import '../data/prefs/prefs_manager.dart';
import '../data/models/sms_job_status.dart';
import 'sms_sender.dart';

@pragma('vm:entry-point')
void startGatewayTask() {
  FlutterForegroundTask.setTaskHandler(GatewayTaskHandler());
}

class GatewayTaskHandler extends TaskHandler {
  late GatewayHttpClient _httpClient;
  late SmsJobRepository _repository;
  late SmsSender _smsSender;
  late PrefsManager _prefs;
  Timer? _pollTimer;
  bool _initialized = false;
  bool _isConnected = false;

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    developer.log('[GatewayTask] Starting gateway task handler...');

    try {
      // Initialize preferences
      _prefs = PrefsManager();
      await _prefs.init();

      // Initialize database
      final database = AppDatabase();
      await database.database;

      // Initialize repository
      _repository = SmsJobRepository(database);

      // Initialize SMS sender
      _smsSender = SmsSender(onResult: _onSmsResult);

      _initialized = true;

      // Get stored URL
      final url = _prefs.getGatewayUrl();
      final deviceId = await _prefs.getDeviceId();

      if (url != null && url.isNotEmpty) {
        developer.log('[GatewayTask] Using URL: $url, Device ID: $deviceId');

        // Create HTTP client
        _httpClient = GatewayHttpClient(
          baseUrl: url,
          deviceId: deviceId,
        );

        // Send device info
        await _sendDeviceInfo();

        // Start polling timer (poll every 5 seconds)
        developer.log('[GatewayTask] Starting polling timer...');
        _pollTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
          developer.log('[GatewayTask] Timer callback - calling _pollForJobs');
          _pollForJobs();
        });
        developer.log('[GatewayTask] Polling timer started');

        // Do initial poll immediately
        developer.log('[GatewayTask] Doing initial poll...');
        await _pollForJobs();

        developer.log('[GatewayTask] Gateway task handler initialized successfully');
      } else {
        developer.log('[GatewayTask] No gateway URL configured');
        await _repository.logWarn('No gateway URL configured');
      }
    } catch (e) {
      developer.log('[GatewayTask] Error during initialization: $e');
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
      // The polling is handled by the timer in onStart
      // This is just a periodic callback from flutter_foreground_task
    } catch (e) {
      developer.log('[GatewayTask] Error in onRepeatEvent: $e');
    }
  }

  @override
  Future<void> onDestroy(DateTime timestamp) async {
    developer.log('[GatewayTask] Destroying gateway task handler');
    _pollTimer?.cancel();
    _smsSender.dispose();
    _httpClient.dispose();
    try {
      await _prefs.setServiceRunning(false);
    } catch (e) {
      developer.log('[GatewayTask] Error setting service running flag: $e');
    }
  }

  void onButtonPressed(String id) {
    // Handle button press if needed
  }

  @override
  void onNotificationButtonPressed(String id) {
    // Handle notification button press
  }

  /// Poll the backend for pending jobs
  Future<void> _pollForJobs() async {
    if (!_initialized) {
      developer.log('[GatewayTask] Poll skipped - not initialized');
      return;
    }

    try {
      developer.log('[GatewayTask] Starting poll cycle...');

      // First, check if backend is healthy
      if (!_isConnected) {
        developer.log('[GatewayTask] Not connected, checking health...');
        final isHealthy = await _httpClient.healthCheck();
        if (!isHealthy) {
          developer.log('[GatewayTask] Backend health check failed');
          if (_isConnected) {
            _isConnected = false;
            await _repository.logWarn('Lost connection to server');
          }
          return;
        }

        // First successful connection
        if (!_isConnected) {
          _isConnected = true;
          await _repository.logInfo('Connected to server');
          developer.log('[GatewayTask] Connected to server');
        }
      }

      // Poll for pending jobs
      developer.log('[GatewayTask] Polling for jobs...');
      final jobs = await _httpClient.getPendingJobs();
      developer.log('[GatewayTask] Poll returned ${jobs.length} jobs');

      if (jobs.isNotEmpty) {
        developer.log('[GatewayTask] Received ${jobs.length} jobs from server');
        await _repository.logInfo('Received ${jobs.length} SMS job(s)');

        for (final job in jobs) {
          developer.log('[GatewayTask] Processing job: ${job.jobId}');

          // Create job in local database if not exists
          final existingJob = await _repository.getJob(job.jobId);
          if (existingJob == null) {
            await _repository.createJob(job);
          }

          // Send the SMS
          await _smsSender.sendSms(
            jobId: job.jobId,
            phoneNumber: job.toNumber,
            messageBody: job.body,
            maxRetries: job.maxRetries,
          );
        }
      }
    } catch (e) {
      developer.log('[GatewayTask] Error polling jobs: $e');
      if (_isConnected) {
        _isConnected = false;
        await _repository.logError('Connection error: $e');
      }
    }
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
        developer.log('[GatewayTask] Job not found: $jobId');
        return;
      }

      developer.log('[GatewayTask] SMS result: $jobId -> ${status.name}');

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

      // Report status to server
      if (_isConnected) {
        await _httpClient.reportStatus(
          jobId: jobId,
          status: status.toJsonString(),
          attempt: updatedJob.attempts,
          errorCode: errorCode,
          errorMessage: errorMessage,
        );
      }
    } catch (e) {
      developer.log('[GatewayTask] Error handling SMS result: $e');
    }
  }

  Future<void> _sendDeviceInfo() async {
    try {
      developer.log('[GatewayTask] Sending device info...');
      final success = await _httpClient.sendDeviceInfo();
      if (success) {
        developer.log('[GatewayTask] ✓ Device info sent successfully');
        await _repository.logInfo('✓ Device registered with server');
      } else {
        developer.log('[GatewayTask] ✗ Failed to send device info');
        await _repository.logWarn('Failed to register device');
      }
    } catch (e) {
      developer.log('[GatewayTask] Error sending device info: $e');
      await _repository.logError('Device registration error: $e');
    }
  }
}
