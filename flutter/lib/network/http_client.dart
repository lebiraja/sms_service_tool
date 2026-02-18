import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';
import '../data/models/sms_job.dart';
import 'dart:developer' as developer;

class GatewayHttpClient {
  final String baseUrl;
  final String deviceId;
  late http.Client _client;

  GatewayHttpClient({
    required this.baseUrl,
    required this.deviceId,
  }) {
    _client = http.Client();
  }

  /// Send device info to backend
  Future<bool> sendDeviceInfo() async {
    try {
      final url = Uri.parse('$baseUrl/api/v1/device/info');
      developer.log('[HttpClient] Sending device info to: $url');

      final response = await _client
          .post(
            url,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'device_id': deviceId,
              'device_name': 'Flutter Device',
              'app_version': '1.0.0',
            }),
          )
          .timeout(const Duration(seconds: 10));

      developer.log('[HttpClient] Device info response: ${response.statusCode}');
      return response.statusCode == 200 || response.statusCode == 201;
    } catch (e) {
      developer.log('[HttpClient] Error sending device info: $e');
      return false;
    }
  }

  /// Poll for pending SMS jobs
  Future<List<SmsJob>> getPendingJobs() async {
    try {
      final url = Uri.parse('$baseUrl/api/v1/jobs/pending?device_id=$deviceId');
      developer.log('[HttpClient] Polling for jobs: $url');

      final response = await _client
          .get(
            url,
            headers: {'Content-Type': 'application/json'},
          )
          .timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        developer.log('[HttpClient] Received ${data.length} pending jobs');

        if (data is List) {
          return data
              .map((job) => SmsJob.fromMap(job as Map<String, dynamic>))
              .toList();
        }
      }
      return [];
    } catch (e) {
      developer.log('[HttpClient] Error polling jobs: $e');
      return [];
    }
  }

  /// Report SMS status to backend
  Future<bool> reportStatus({
    required String jobId,
    required String status,
    required int attempt,
    int? errorCode,
    String? errorMessage,
  }) async {
    try {
      final url = Uri.parse('$baseUrl/api/v1/jobs/$jobId/status?device_id=$deviceId');
      developer.log('[HttpClient] Reporting status for $jobId: $status');

      final response = await _client
          .post(
            url,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'status': status,
              'attempt': attempt,
              'error_code': errorCode,
              'error_message': errorMessage,
              'timestamp': DateTime.now().toIso8601String(),
            }),
          )
          .timeout(const Duration(seconds: 10));

      developer.log('[HttpClient] Status report response: ${response.statusCode}');
      return response.statusCode == 200 || response.statusCode == 201;
    } catch (e) {
      developer.log('[HttpClient] Error reporting status: $e');
      return false;
    }
  }

  /// Health check
  Future<bool> healthCheck() async {
    try {
      final url = Uri.parse('$baseUrl/health');
      final response = await _client
          .get(url)
          .timeout(const Duration(seconds: 5));
      return response.statusCode == 200;
    } catch (e) {
      developer.log('[HttpClient] Health check failed: $e');
      return false;
    }
  }

  void dispose() {
    _client.close();
  }
}
