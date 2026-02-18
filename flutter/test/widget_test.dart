// This is a basic Flutter widget test for the SMS Gateway app.

import 'package:flutter_test/flutter_test.dart';
import 'package:smstool_gateway/app.dart';
import 'package:smstool_gateway/data/database/app_database.dart';
import 'package:smstool_gateway/data/repository/sms_job_repository.dart';
import 'package:smstool_gateway/data/prefs/prefs_manager.dart';
import 'package:smstool_gateway/ui/viewmodels/main_viewmodel.dart';

void main() {
  testWidgets('SMS Gateway app smoke test', (WidgetTester tester) async {
    // Create required dependencies
    final prefs = PrefsManager();
    final database = AppDatabase();
    final repository = SmsJobRepository(database);

    final viewModel = MainViewModel(
      prefs: prefs,
      repository: repository,
    );

    // Build our app and trigger a frame.
    await tester.pumpWidget(GatewayApp(viewModel: viewModel));

    // Verify that the app title appears
    expect(find.text('SMS Gateway'), findsOneWidget);

    // Verify that the main screen widgets are present
    expect(find.text('Backend Server'), findsOneWidget);
    expect(find.text('Device ID'), findsOneWidget);
    expect(find.text('Activity Log'), findsOneWidget);
    expect(find.text('Connect'), findsOneWidget);
  });
}
