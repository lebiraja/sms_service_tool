import 'package:sqflite/sqflite.dart';
import '../models/sms_job.dart';
import '../models/sms_job_status.dart';
import 'app_database.dart';

class SmsJobDao {
  final AppDatabase _db;

  SmsJobDao(this._db);

  Future<void> insert(SmsJob job) async {
    final db = await _db.database;
    await db.insert(
      'sms_jobs',
      job.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<void> updateStatus(String jobId, SmsJob updatedJob) async {
    final db = await _db.database;
    await db.update(
      'sms_jobs',
      updatedJob.toMap(),
      where: 'jobId = ?',
      whereArgs: [jobId],
    );
  }

  Future<SmsJob?> getById(String jobId) async {
    final db = await _db.database;
    final results = await db.query(
      'sms_jobs',
      where: 'jobId = ?',
      whereArgs: [jobId],
    );
    if (results.isEmpty) return null;
    return SmsJob.fromMap(results.first);
  }

  Future<List<SmsJob>> getPendingRetries() async {
    final db = await _db.database;
    final now = DateTime.now().millisecondsSinceEpoch;
    final results = await db.query(
      'sms_jobs',
      where:
          "status = 'failed_retrying' AND (nextRetryAt IS NULL OR nextRetryAt <= ?)",
      whereArgs: [now],
    );
    return results.map((r) => SmsJob.fromMap(r)).toList();
  }

  Future<List<SmsJob>> getPendingReports() async {
    final db = await _db.database;
    final results = await db.query(
      'sms_jobs',
      where: "pendingReport = 1",
    );
    return results.map((r) => SmsJob.fromMap(r)).toList();
  }

  Future<void> clearPendingReport(String jobId) async {
    final db = await _db.database;
    await db.update(
      'sms_jobs',
      {'pendingReport': 0},
      where: 'jobId = ?',
      whereArgs: [jobId],
    );
  }

  Future<void> delete(String jobId) async {
    final db = await _db.database;
    await db.delete(
      'sms_jobs',
      where: 'jobId = ?',
      whereArgs: [jobId],
    );
  }
}
