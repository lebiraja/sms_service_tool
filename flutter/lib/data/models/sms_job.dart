import 'sms_job_status.dart';

class SmsJob {
  final String jobId;
  final String toNumber;
  final String body;
  final SmsJobStatus status;
  final int attempts;
  final int maxRetries;
  final DateTime createdAt;
  final DateTime updatedAt;
  final DateTime? sentAt;
  final DateTime? deliveredAt;
  final int? errorCode;
  final String? errorMessage;
  final DateTime? nextRetryAt;
  final bool pendingReport;

  SmsJob({
    required this.jobId,
    required this.toNumber,
    required this.body,
    required this.status,
    required this.attempts,
    required this.maxRetries,
    required this.createdAt,
    required this.updatedAt,
    this.sentAt,
    this.deliveredAt,
    this.errorCode,
    this.errorMessage,
    this.nextRetryAt,
    this.pendingReport = false,
  });

  SmsJob copyWith({
    String? jobId,
    String? toNumber,
    String? body,
    SmsJobStatus? status,
    int? attempts,
    int? maxRetries,
    DateTime? createdAt,
    DateTime? updatedAt,
    DateTime? sentAt,
    DateTime? deliveredAt,
    int? errorCode,
    String? errorMessage,
    DateTime? nextRetryAt,
    bool? pendingReport,
  }) =>
      SmsJob(
        jobId: jobId ?? this.jobId,
        toNumber: toNumber ?? this.toNumber,
        body: body ?? this.body,
        status: status ?? this.status,
        attempts: attempts ?? this.attempts,
        maxRetries: maxRetries ?? this.maxRetries,
        createdAt: createdAt ?? this.createdAt,
        updatedAt: updatedAt ?? this.updatedAt,
        sentAt: sentAt ?? this.sentAt,
        deliveredAt: deliveredAt ?? this.deliveredAt,
        errorCode: errorCode ?? this.errorCode,
        errorMessage: errorMessage ?? this.errorMessage,
        nextRetryAt: nextRetryAt ?? this.nextRetryAt,
        pendingReport: pendingReport ?? this.pendingReport,
      );

  // Convert to database map
  Map<String, dynamic> toMap() => {
        'jobId': jobId,
        'toNumber': toNumber,
        'body': body,
        'status': status.toJsonString(),
        'attempts': attempts,
        'maxRetries': maxRetries,
        'createdAt': createdAt.millisecondsSinceEpoch,
        'updatedAt': updatedAt.millisecondsSinceEpoch,
        'sentAt': sentAt?.millisecondsSinceEpoch,
        'deliveredAt': deliveredAt?.millisecondsSinceEpoch,
        'errorCode': errorCode,
        'errorMessage': errorMessage,
        'nextRetryAt': nextRetryAt?.millisecondsSinceEpoch,
        'pendingReport': pendingReport ? 1 : 0,
      };

  // Create from database map
  factory SmsJob.fromMap(Map<String, dynamic> map) => SmsJob(
        jobId: map['jobId'] as String,
        toNumber: map['toNumber'] as String,
        body: map['body'] as String,
        status: SmsJobStatusExt.fromJsonString(map['status'] as String),
        attempts: map['attempts'] as int,
        maxRetries: map['maxRetries'] as int,
        createdAt:
            DateTime.fromMillisecondsSinceEpoch(map['createdAt'] as int),
        updatedAt:
            DateTime.fromMillisecondsSinceEpoch(map['updatedAt'] as int),
        sentAt: map['sentAt'] != null
            ? DateTime.fromMillisecondsSinceEpoch(map['sentAt'] as int)
            : null,
        deliveredAt: map['deliveredAt'] != null
            ? DateTime.fromMillisecondsSinceEpoch(map['deliveredAt'] as int)
            : null,
        errorCode: map['errorCode'] as int?,
        errorMessage: map['errorMessage'] as String?,
        nextRetryAt: map['nextRetryAt'] != null
            ? DateTime.fromMillisecondsSinceEpoch(map['nextRetryAt'] as int)
            : null,
        pendingReport: (map['pendingReport'] as int?) == 1,
      );
}
