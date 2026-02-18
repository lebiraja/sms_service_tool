import 'package:sqflite/sqflite.dart';
import '../models/event_log_entry.dart';
import 'app_database.dart';

const _maxRows = 500;

class EventLogDao {
  final AppDatabase _db;

  EventLogDao(this._db);

  Future<void> insert(EventLogEntry entry) async {
    final db = await _db.database;

    // Insert new entry
    await db.insert(
      'event_log',
      entry.toMap(),
      conflictAlgorithm: ConflictAlgorithm.replace,
    );

    // Delete oldest entries if over limit
    final count = Sqflite.firstIntValue(
      await db.rawQuery('SELECT COUNT(*) FROM event_log'),
    );
    if ((count ?? 0) > _maxRows) {
      final toDelete = (count ?? 0) - _maxRows;
      await db.delete(
        'event_log',
        where: 'id IN (SELECT id FROM event_log ORDER BY id ASC LIMIT ?)',
        whereArgs: [toDelete],
      );
    }
  }

  Future<List<EventLogEntry>> getLatest(int limit) async {
    final db = await _db.database;
    final results = await db.query(
      'event_log',
      orderBy: 'timestamp DESC',
      limit: limit,
    );
    return results.map((r) => EventLogEntry.fromMap(r)).toList().reversed.toList();
  }

  Future<void> clear() async {
    final db = await _db.database;
    await db.delete('event_log');
  }
}
