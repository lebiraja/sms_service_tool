import 'package:flutter/material.dart';
import '../../data/models/event_log_entry.dart';
import 'event_log_item.dart';

class EventLogList extends StatelessWidget {
  final List<EventLogEntry> logs;

  const EventLogList({required this.logs, super.key});

  @override
  Widget build(BuildContext context) {
    if (logs.isEmpty) {
      return const Center(
        child: Text('No activity yet'),
      );
    }

    return ListView.builder(
      itemCount: logs.length,
      reverse: true,
      itemBuilder: (context, index) => EventLogItem(entry: logs[index]),
    );
  }
}
