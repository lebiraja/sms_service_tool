import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:developer' as developer;
import '../../data/models/event_log_entry.dart';
import '../../data/prefs/prefs_manager.dart';
import '../../data/repository/sms_job_repository.dart';
import '../../service/gateway_task_handler.dart';

enum ConnectionStateEnum {
  idle,
  connecting,
  connected,
  error,
}

class MainViewModel extends ChangeNotifier {
  final PrefsManager _prefs;
  final SmsJobRepository _repository;

  ConnectionStateEnum _connectionState = ConnectionStateEnum.idle;
  String _serverUrl = '';
  String _deviceId = '';
  String? _errorMessage;
  List<EventLogEntry> _eventLog = [];

  MainViewModel({
    required PrefsManager prefs,
    required SmsJobRepository repository,
  })  : _prefs = prefs,
        _repository = repository {
    _init();
  }

  // Getters
  ConnectionStateEnum get connectionState => _connectionState;
  String get serverUrl => _serverUrl;
  String get deviceId => _deviceId;
  String? get errorMessage => _errorMessage;
  List<EventLogEntry> get eventLog => _eventLog;

  Future<void> _init() async {
    _deviceId = await _prefs.getDeviceId();
    _serverUrl = _prefs.getGatewayUrl() ?? '';

    // Listen to log updates
    _repository.getLogsStream(limit: 50).listen((logs) {
      _eventLog = logs;
      notifyListeners();
    });

    notifyListeners();
  }

  void updateServerUrl(String url) {
    _serverUrl = url;
    notifyListeners();
  }

  Future<void> connect() async {
    if (_serverUrl.isEmpty) {
      _errorMessage = 'Please enter a server URL';
      notifyListeners();
      return;
    }

    _errorMessage = null;
    _connectionState = ConnectionStateEnum.connecting;
    notifyListeners();

    try {
      // Normalize URL
      String normalizedUrl = _serverUrl.trim();
      if (!normalizedUrl.startsWith('http://') && !normalizedUrl.startsWith('https://')) {
        normalizedUrl = 'https://$normalizedUrl';
      }

      // Quick health check
      developer.log('Checking server health: $normalizedUrl');
      final client = http.Client();
      bool healthOk = false;
      try {
        final response = await client
            .get(Uri.parse('$normalizedUrl/health'))
            .timeout(const Duration(seconds: 5));

        if (response.statusCode != 200) {
          throw Exception('Server returned ${response.statusCode}');
        }
        healthOk = true;
      } finally {
        client.close();
      }

      if (!healthOk) {
        throw Exception('Health check failed');
      }

      // Save URL
      await _prefs.setGatewayUrl(normalizedUrl);
      await _prefs.setServiceRunning(true);

      // Register device immediately
      developer.log('Registering device with server...');
      await _registerDevice(normalizedUrl);

      // Start foreground service
      await _startForegroundService();

      _connectionState = ConnectionStateEnum.connected;
      await _repository.logInfo('Connected to server: $normalizedUrl');
      notifyListeners();
    } catch (e) {
      _errorMessage = 'Failed to connect: $e';
      _connectionState = ConnectionStateEnum.error;
      await _repository.logError('Connection failed: $e');
      notifyListeners();
    }
  }

  Future<void> _registerDevice(String baseUrl) async {
    try {
      final client = http.Client();
      try {
        final response = await client
            .post(
              Uri.parse('$baseUrl/api/v1/device/info'),
              headers: {'Content-Type': 'application/json'},
              body: jsonEncode({
                'device_id': _deviceId,
                'device_name': 'Flutter Device',
                'app_version': '1.0.0',
              }),
            )
            .timeout(const Duration(seconds: 5));

        if (response.statusCode == 200 || response.statusCode == 201) {
          developer.log('Device registered successfully');
          await _repository.logInfo('Device registered: $_deviceId');
        } else {
          throw Exception('Device registration returned ${response.statusCode}');
        }
      } finally {
        client.close();
      }
    } catch (e) {
      developer.log('Error registering device: $e');
      // Don't fail connection if device registration fails
      await _repository.logWarn('Device registration warning: $e');
    }
  }

  Future<void> disconnect() async {
    await _stopForegroundService();
    await _prefs.setServiceRunning(false);
    _connectionState = ConnectionStateEnum.idle;
    await _repository.logInfo('Disconnected from server');
    notifyListeners();
  }

  Future<void> _startForegroundService() async {
    try {
      developer.log('Initializing foreground task...');

      // Configure foreground task
      FlutterForegroundTask.init(
        androidNotificationOptions: AndroidNotificationOptions(
          id: 1,
          channelId: 'sms_gateway_channel',
          channelName: 'SMS Gateway',
          channelDescription: 'SMS Gateway background service',
        ),
        iosNotificationOptions: const IOSNotificationOptions(
          showNotification: true,
          playSound: false,
        ),
        foregroundTaskOptions: ForegroundTaskOptions(
          eventAction: ForegroundTaskEventAction.repeat(5000),
          autoRunOnBoot: true,
          allowWakeLock: true,
          allowWifiLock: true,
        ),
      );

      developer.log('Starting foreground service...');

      // Start the foreground task
      await FlutterForegroundTask.startService(
        serviceId: 100,
        notificationTitle: 'SMS Gateway',
        notificationText: 'Polling server for SMS jobs',
        callback: startGatewayTask,
      );

      developer.log('Foreground service started successfully');
      notifyListeners();
    } catch (e) {
      developer.log('Error starting foreground service: $e');
      _errorMessage = 'Failed to start service: $e';
      _connectionState = ConnectionStateEnum.error;
      notifyListeners();
      rethrow;
    }
  }

  Future<void> _stopForegroundService() async {
    try {
      await FlutterForegroundTask.stopService();
    } catch (e) {
      developer.log('Error stopping foreground service: $e');
    }
  }
}
