import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

class PrefsManager {
  static PrefsManager? _instance;
  late SharedPreferences _prefs;

  PrefsManager._();

  factory PrefsManager() {
    _instance ??= PrefsManager._();
    return _instance!;
  }

  Future<void> init() async {
    _prefs = await SharedPreferences.getInstance();
  }

  // Device ID - generated once, never changes
  Future<String> getDeviceId() async {
    String? id = _prefs.getString('device_id');
    if (id == null) {
      id = const Uuid().v4();
      await _prefs.setString('device_id', id);
    }
    return id;
  }

  // Gateway URL - user configurable
  String? getGatewayUrl() => _prefs.getString('gateway_url');

  Future<void> setGatewayUrl(String url) async {
    await _prefs.setString('gateway_url', url);
  }

  // Service running flag - for boot receiver
  bool isServiceRunning() => _prefs.getBool('service_running') ?? false;

  Future<void> setServiceRunning(bool running) async {
    await _prefs.setBool('service_running', running);
  }
}
