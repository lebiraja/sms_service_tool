import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../viewmodels/main_viewmodel.dart';
import '../widgets/status_chip.dart';
import '../widgets/event_log_list.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  late TextEditingController _urlController;

  @override
  void initState() {
    super.initState();
    _urlController = TextEditingController();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final viewModel = context.read<MainViewModel>();
      _urlController.text = viewModel.serverUrl;
    });
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  void _copyDeviceId(String deviceId) {
    Clipboard.setData(ClipboardData(text: deviceId));
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Device ID copied')),
    );
  }

  String _truncateMiddle(String str) {
    if (str.length <= 20) return str;
    return '${str.substring(0, 8)}...${str.substring(str.length - 8)}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SMS Gateway'),
        elevation: 2,
        backgroundColor: Colors.blue[600],
      ),
      body: Consumer<MainViewModel>(
        builder: (context, viewModel, _) {
          return SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Status Card
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey[300]!),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        StatusChip(state: viewModel.connectionState),
                        const SizedBox(height: 8),
                        Text(
                          _getDeviceInfoText(viewModel),
                          style: const TextStyle(
                            fontSize: 12,
                            color: Colors.grey,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Server Configuration
                  const Text(
                    'Backend Server',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _urlController,
                    enabled: !viewModel.isConnected() && !viewModel.isConnecting(),
                    decoration: InputDecoration(
                      hintText: 'e.g., 192.168.1.100:7777 or ws://example.com',
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(4),
                      ),
                    ),
                    onChanged: viewModel.updateServerUrl,
                  ),
                  const SizedBox(height: 16),

                  // Device ID
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      const Text(
                        'Device ID',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: SelectableText(
                          _truncateMiddle(viewModel.deviceId),
                          style: const TextStyle(
                            fontSize: 12,
                            fontFamily: 'monospace',
                          ),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.content_copy),
                        iconSize: 20,
                        onPressed: () => _copyDeviceId(viewModel.deviceId),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),

                  // Connect Button
                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: FilledButton(
                      onPressed: _getButtonOnPressed(viewModel),
                      child: Text(_getButtonLabel(viewModel)),
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Error message
                  if (viewModel.errorMessage != null)
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.red[50],
                        border: Border.all(color: Colors.red[200]!),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        viewModel.errorMessage!,
                        style: TextStyle(
                          color: Colors.red[600],
                          fontSize: 12,
                        ),
                      ),
                    ),
                  const SizedBox(height: 24),

                  // Activity Log
                  const Text(
                    'Activity Log',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    height: 300,
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey[300]!),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: EventLogList(logs: viewModel.eventLog),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  String _getDeviceInfoText(MainViewModel viewModel) {
    switch (viewModel.connectionState) {
      case ConnectionStateEnum.idle:
        return '';
      case ConnectionStateEnum.connecting:
        return 'Connecting...';
      case ConnectionStateEnum.connected:
        return 'Connected to ${viewModel.serverUrl}';
      case ConnectionStateEnum.error:
        return 'Error: ${viewModel.errorMessage ?? 'Unknown error'}';
    }
  }

  String _getButtonLabel(MainViewModel viewModel) {
    switch (viewModel.connectionState) {
      case ConnectionStateEnum.idle:
      case ConnectionStateEnum.error:
        return 'Connect';
      case ConnectionStateEnum.connecting:
        return 'Cancel';
      case ConnectionStateEnum.connected:
        return 'Disconnect';
    }
  }

  VoidCallback? _getButtonOnPressed(MainViewModel viewModel) {
    switch (viewModel.connectionState) {
      case ConnectionStateEnum.idle:
      case ConnectionStateEnum.error:
        return () => viewModel.connect();
      case ConnectionStateEnum.connecting:
        return () => viewModel.disconnect();
      case ConnectionStateEnum.connected:
        return () => viewModel.disconnect();
    }
  }
}
