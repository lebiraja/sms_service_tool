import 'package:uuid/uuid.dart';
import 'dart:convert';

abstract class ServerMessage {
  const ServerMessage();
}

class SmsJobMessage extends ServerMessage {
  final String jobId;
  final String to;
  final String body;
  final int maxRetries;

  const SmsJobMessage({
    required this.jobId,
    required this.to,
    required this.body,
    required this.maxRetries,
  });

  factory SmsJobMessage.fromJson(Map<String, dynamic> json) => SmsJobMessage(
        jobId: json['job_id'] ?? json['jobId'] ?? '',
        to: json['to'] ?? '',
        body: json['body'] ?? '',
        maxRetries: json['max_retries'] ?? json['maxRetries'] ?? 3,
      );
}

class PingMessage extends ServerMessage {
  final String messageId;

  const PingMessage(this.messageId);

  factory PingMessage.fromJson(Map<String, dynamic> json) =>
      PingMessage(json['message_id'] ?? '');
}

class ErrorMessage extends ServerMessage {
  final String code;
  final String detail;

  const ErrorMessage({
    required this.code,
    required this.detail,
  });

  factory ErrorMessage.fromJson(Map<String, dynamic> json) => ErrorMessage(
        code: json['code'] ?? 'UNKNOWN',
        detail: json['detail'] ?? '',
      );
}

class MessageParser {
  static ServerMessage parseServerMessage(String jsonStr) {
    try {
      final json = jsonDecode(jsonStr) as Map<String, dynamic>;
      final type = json['type'] as String?;

      switch (type) {
        case 'sms_job':
          return SmsJobMessage.fromJson(json);
        case 'ping':
          return PingMessage.fromJson(json);
        case 'error':
          return ErrorMessage.fromJson(json);
        default:
          throw Exception('Unknown message type: $type');
      }
    } catch (e) {
      throw Exception('Failed to parse server message: $e');
    }
  }

  // Build device_info message for server
  static String buildDeviceInfoMessage({
    required String deviceId,
    required String deviceName,
    required int androidVersion,
    String appVersion = '1.0.0',
  }) {
    return jsonEncode({
      'type': 'device_info',
      'message_id': const Uuid().v4(),
      'device_id': deviceId,
      'device_name': deviceName,
      'android_version': androidVersion,
      'app_version': appVersion,
      'connected_at': DateTime.now().toIso8601String(),
    });
  }

  // Build status_update message for server
  static String buildStatusUpdateMessage({
    required String jobId,
    required String status,
    required int attempt,
    int? errorCode,
    String? errorMessage,
  }) {
    return jsonEncode({
      'type': 'status_update',
      'message_id': const Uuid().v4(),
      'job_id': jobId,
      'status': status,
      'attempt': attempt,
      'error_code': errorCode,
      'error_message': errorMessage,
      'timestamp': DateTime.now().toIso8601String(),
    });
  }

  // Build pong message for server
  static String buildPongMessage(String pingMessageId) {
    return jsonEncode({
      'type': 'pong',
      'message_id': const Uuid().v4(),
      'ping_message_id': pingMessageId,
      'timestamp': DateTime.now().toIso8601String(),
    });
  }
}
