import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'ui/screens/main_screen.dart';
import 'ui/viewmodels/main_viewmodel.dart';

class GatewayApp extends StatelessWidget {
  final MainViewModel viewModel;

  const GatewayApp({required this.viewModel, super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SMS Gateway',
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
      ),
      home: ChangeNotifierProvider<MainViewModel>.value(
        value: viewModel,
        child: const MainScreen(),
      ),
    );
  }
}
