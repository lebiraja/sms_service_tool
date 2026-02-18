enum EventLogLevel {
  info,
  warn,
  error,
}

extension EventLogLevelExt on EventLogLevel {
  String toJsonString() => name;

  static EventLogLevel fromJsonString(String value) {
    return EventLogLevel.values.firstWhere(
      (e) => e.name == value,
      orElse: () => EventLogLevel.info,
    );
  }

  String get iconChar {
    switch (this) {
      case EventLogLevel.info:
        return 'ℹ';
      case EventLogLevel.warn:
        return '⚠';
      case EventLogLevel.error:
        return '✕';
    }
  }
}

class EventLogEntry {
  final int? id;
  final DateTime timestamp;
  final EventLogLevel level;
  final String message;

  EventLogEntry({
    this.id,
    required this.timestamp,
    required this.level,
    required this.message,
  });

  Map<String, dynamic> toMap() => {
        'id': id,
        'timestamp': timestamp.millisecondsSinceEpoch,
        'level': level.toJsonString(),
        'message': message,
      };

  factory EventLogEntry.fromMap(Map<String, dynamic> map) => EventLogEntry(
        id: map['id'] as int?,
        timestamp:
            DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int),
        level: EventLogLevelExt.fromJsonString(map['level'] as String),
        message: map['message'] as String,
      );
}
