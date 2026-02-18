import 'package:flutter/material.dart';
import '../viewmodels/main_viewmodel.dart';

class StatusChip extends StatelessWidget {
  final ConnectionStateEnum state;

  const StatusChip({required this.state, Key? key}) : super(key: key);

  Color _getColor() {
    switch (state) {
      case ConnectionStateEnum.idle:
        return Colors.grey[600]!;
      case ConnectionStateEnum.connecting:
        return Colors.orange[300]!;
      case ConnectionStateEnum.connected:
        return Colors.green[400]!;
      case ConnectionStateEnum.error:
        return Colors.red[400]!;
    }
  }

  String _getText() {
    switch (state) {
      case ConnectionStateEnum.idle:
        return 'Not connected';
      case ConnectionStateEnum.connecting:
        return 'Connecting...';
      case ConnectionStateEnum.connected:
        return 'Connected';
      case ConnectionStateEnum.error:
        return 'Connection failed';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Chip(
      avatar: CircleAvatar(
        backgroundColor: _getColor(),
        radius: 4,
      ),
      label: Text(
        _getText(),
        style: const TextStyle(
          color: Colors.white,
          fontWeight: FontWeight.w500,
        ),
      ),
      backgroundColor: _getColor(),
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
    );
  }
}
