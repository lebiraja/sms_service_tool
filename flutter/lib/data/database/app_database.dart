import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as p;
import 'sms_job_dao.dart';
import 'event_log_dao.dart';

const _dbName = 'smstool.db';
const _version = 1;

class AppDatabase {
  static AppDatabase? _instance;
  static Database? _database;

  AppDatabase._();

  factory AppDatabase() {
    _instance ??= AppDatabase._();
    return _instance!;
  }

  Future<Database> get database async {
    _database ??= await _initDb();
    return _database!;
  }

  Future<Database> _initDb() async {
    final dbPath = await getDatabasesPath();
    final path = p.join(dbPath, _dbName);

    return openDatabase(
      path,
      version: _version,
      onCreate: _createTables,
    );
  }

  Future<void> _createTables(Database db, int version) async {
    await db.execute('''
      CREATE TABLE sms_jobs (
        jobId TEXT PRIMARY KEY,
        toNumber TEXT NOT NULL,
        body TEXT NOT NULL,
        status TEXT NOT NULL,
        attempts INTEGER NOT NULL,
        maxRetries INTEGER NOT NULL,
        createdAt INTEGER NOT NULL,
        updatedAt INTEGER NOT NULL,
        sentAt INTEGER,
        deliveredAt INTEGER,
        errorCode INTEGER,
        errorMessage TEXT,
        nextRetryAt INTEGER,
        pendingReport INTEGER NOT NULL DEFAULT 0
      )
    ''');

    await db.execute('''
      CREATE TABLE event_log (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER NOT NULL,
        level TEXT NOT NULL,
        message TEXT NOT NULL
      )
    ''');
  }

  SmsJobDao get smsJobDao => SmsJobDao(this);
  EventLogDao get eventLogDao => EventLogDao(this);

  Future<void> close() async {
    _database?.close();
    _database = null;
  }
}
