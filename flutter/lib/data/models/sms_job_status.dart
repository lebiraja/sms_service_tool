enum SmsJobStatus {
  queued,
  sending,
  sent,
  delivered,
  failedRetrying,
  failedPermanent,
}

extension SmsJobStatusExt on SmsJobStatus {
  String toJsonString() => name;

  static SmsJobStatus fromJsonString(String value) {
    return SmsJobStatus.values.firstWhere(
      (e) => e.name == value,
      orElse: () => SmsJobStatus.queued,
    );
  }

  bool get isFinal =>
      this == SmsJobStatus.sent ||
      this == SmsJobStatus.delivered ||
      this == SmsJobStatus.failedPermanent;
}
