import 'package:flutter/services.dart';
import '../data/models/sms_job.dart';
import '../data/models/sms_job_status.dart';

typedef OnSmsResult = void Function(
  String jobId,
  SmsJobStatus status,
  int? errorCode,
  String? errorMessage,
);

class SmsSender {
  static const platform = MethodChannel('com.smstool.gateway/sms');
  static const deliveryChannel =
      EventChannel('com.smstool.gateway/smsDeliveryEvents');

  final OnSmsResult onResult;

  SmsSender({required this.onResult}) {
    _initDeliveryListener();
  }

  void _initDeliveryListener() {
    deliveryChannel.receiveBroadcastStream().listen(
      (event) {
        if (event is Map) {
          final jobId = event['jobId'] as String?;
          final statusStr = event['status'] as String?;
          final errorCode = event['errorCode'] as int?;
          final errorMessage = event['errorMessage'] as String?;

          if (jobId != null && statusStr != null) {
            final status = SmsJobStatusExt.fromJsonString(statusStr);
            onResult(jobId, status, errorCode, errorMessage);
          }
        }
      },
      onError: (error) {
        print('Delivery stream error: $error');
      },
    );
  }

  Future<void> sendSms({
    required String jobId,
    required String phoneNumber,
    required String messageBody,
    required int maxRetries,
  }) async {
    try {
      await platform.invokeMethod('sendSms', {
        'jobId': jobId,
        'to': phoneNumber,
        'body': messageBody,
        'maxRetries': maxRetries,
      });
    } catch (e) {
      print('Error sending SMS: $e');
      onResult(jobId, SmsJobStatus.failedPermanent, null, e.toString());
    }
  }

  void dispose() {
    // Clean up if needed
  }
}
