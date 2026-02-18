import 'package:flutter/material.dart';
import '../../data/models/event_log_entry.dart';

class EventLogItem extends StatelessWidget {
  final EventLogEntry entry;

  const EventLogItem({required this.entry, super.key});

  Color _getIconColor() {
    switch (entry.level) {
      case EventLogLevel.info:
        return Colors.blue[300]!;
      case EventLogLevel.warn:
        return Colors.orange[400]!;
      case EventLogLevel.error:
        return Colors.red[400]!;
    }
  }

  String _formatTime(DateTime dt) {
    return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}:${dt.second.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 50,
            child: Text(
              _formatTime(entry.timestamp),
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
                fontFamily: 'monospace',
              ),
            ),
          ),
          SizedBox(
            width: 20,
            child: Text(
              entry.level.iconChar,
              style: TextStyle(
                fontSize: 12,
                color: _getIconColor(),
              ),
              textAlign: TextAlign.center,
            ),
          ),
          Expanded(
            child: Text(
              entry.message,
              style: const TextStyle(fontSize: 13),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
