import '../database/app_database.dart';
import '../models/event_log_entry.dart';
import '../models/sms_job.dart';

class SmsJobRepository {
  final AppDatabase _database;

  SmsJobRepository(this._database);

  Future<void> createJob(SmsJob job) async {
    await _database.smsJobDao.insert(job);
    await _logEvent(
      EventLogLevel.info,
      'Job queued: ${job.jobId} → ${job.toNumber}',
    );
  }

  Future<void> updateJobStatus(
    String jobId,
    SmsJob updatedJob, {
    String? logMessage,
  }) async {
    await _database.smsJobDao.updateStatus(jobId, updatedJob);
    if (logMessage != null) {
      await _logEvent(EventLogLevel.info, logMessage);
    }
  }

  Future<SmsJob?> getJob(String jobId) {
    return _database.smsJobDao.getById(jobId);
  }

  Future<List<SmsJob>> getPendingRetries() {
    return _database.smsJobDao.getPendingRetries();
  }

  Future<List<SmsJob>> getPendingReports() {
    return _database.smsJobDao.getPendingReports();
  }

  Future<void> clearPendingReport(String jobId) {
    return _database.smsJobDao.clearPendingReport(jobId);
  }

  Future<void> _logEvent(EventLogLevel level, String message) async {
    await _database.eventLogDao.insert(
      EventLogEntry(
        timestamp: DateTime.now(),
        level: level,
        message: message,
      ),
    );
  }

  Future<void> logInfo(String message) => _logEvent(EventLogLevel.info, message);

  Future<void> logWarn(String message) => _logEvent(EventLogLevel.warn, message);

  Future<void> logError(String message) => _logEvent(EventLogLevel.error, message);

  Future<List<EventLogEntry>> getLatestLogs(int limit) {
    return _database.eventLogDao.getLatest(limit);
  }

  Stream<List<EventLogEntry>> getLogsStream({int limit = 50}) async* {
    while (true) {
      final logs = await getLatestLogs(limit);
      yield logs;
      await Future.delayed(const Duration(seconds: 1));
    }
  }
}
