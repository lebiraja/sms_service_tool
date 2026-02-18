import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';
import '../../data/models/event_log_entry.dart';
import '../../data/prefs/prefs_manager.dart';
import '../../data/repository/sms_job_repository.dart';
import '../../network/websocket_manager.dart';
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
  final WebSocketManager _wsManager;

  ConnectionStateEnum _connectionState = ConnectionStateEnum.idle;
  String _serverUrl = '';
  String _deviceId = '';
  String? _errorMessage;
  List<EventLogEntry> _eventLog = [];

  MainViewModel({
    required PrefsManager prefs,
    required SmsJobRepository repository,
    required WebSocketManager wsManager,
  })  : _prefs = prefs,
        _repository = repository,
        _wsManager = wsManager {
    _wsManager.onStateChange = _onWsStateChange;
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

  void _onWsStateChange(ConnectionState state, String? error) {
    _errorMessage = error;

    switch (state) {
      case ConnectionState.idle:
        _connectionState = ConnectionStateEnum.idle;
      case ConnectionState.connecting:
        _connectionState = ConnectionStateEnum.connecting;
      case ConnectionState.connected:
        _connectionState = ConnectionStateEnum.connected;
      case ConnectionState.error:
        _connectionState = ConnectionStateEnum.error;
    }

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
    await _prefs.setGatewayUrl(_serverUrl);
    await _prefs.setServiceRunning(true);

    try {
      // Configure and start the foreground service
      await _startForegroundService();
    } catch (e) {
      _errorMessage = 'Failed to start service: $e';
      notifyListeners();
    }
  }

  Future<void> disconnect() async {
    await _stopForegroundService();
    await _prefs.setServiceRunning(false);
  }

  Future<void> _startForegroundService() async {
    try {
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

      // Start the foreground task
      await FlutterForegroundTask.startService(
        serviceId: 100,
        notificationTitle: 'SMS Gateway',
        notificationText: 'Connected to gateway server',
        callback: startGatewayTask,
      );

      notifyListeners();
    } catch (e) {
      print('Error starting foreground service: $e');
      rethrow;
    }
  }

  Future<void> _stopForegroundService() async {
    try {
      await FlutterForegroundTask.stopService();
    } catch (e) {
      print('Error stopping foreground service: $e');
    }
  }

  void copyDeviceId() {
    // Will be called from UI with Clipboard.setData()
  }

  bool isConnecting() => _connectionState == ConnectionStateEnum.connecting;
  bool isConnected() => _connectionState == ConnectionStateEnum.connected;
  bool isIdle() => _connectionState == ConnectionStateEnum.idle;
  bool hasError() => _connectionState == ConnectionStateEnum.error;
}
