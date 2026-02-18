import 'package:flutter/material.dart';
import 'app.dart';
import 'data/database/app_database.dart';
import 'data/repository/sms_job_repository.dart';
import 'data/prefs/prefs_manager.dart';
import 'ui/viewmodels/main_viewmodel.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Initialize SharedPreferences
  final prefs = PrefsManager();
  await prefs.init();

  // Initialize database
  final database = AppDatabase();
  await database.database;

  // Create repository
  final repository = SmsJobRepository(database);

  // Create ViewModel
  final viewModel = MainViewModel(
    prefs: prefs,
    repository: repository,
  );

  await Future<void>.delayed(const Duration(milliseconds: 500)); // ignore: unused_result

  runApp(GatewayApp(viewModel: viewModel));
}
