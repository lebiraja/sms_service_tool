package com.smstool.gateway;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.smstool.gateway.service.GatewayForegroundService;
import com.smstool.gateway.ui.adapter.EventLogAdapter;
import com.smstool.gateway.ui.viewmodel.MainViewModel;

/**
 * MainActivity - The main UI screen for the SMS Gateway app.
 * Displays connection status, allows URL configuration, and shows activity log.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Views
    private Toolbar toolbar;
    private Chip statusChip;
    private TextView deviceInfoText;
    private TextInputEditText urlInput;
    private TextView deviceIdText;
    private ImageButton copyDeviceIdButton;
    private MaterialButton connectButton;
    private MaterialTextView errorText;
    private RecyclerView eventLogRecycler;

    // ViewModel & Adapter
    private MainViewModel viewModel;
    private EventLogAdapter eventLogAdapter;

    // Service binding
    private GatewayForegroundService boundService;
    private boolean isBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GatewayForegroundService.ServiceBinder binder = (GatewayForegroundService.ServiceBinder) service;
            boundService = binder.getService();
            isBound = true;
            Log.i(TAG, "Service bound");

            // Set up service state listener
            boundService.setStateListener(new GatewayForegroundService.ServiceStateListener() {
                @Override
                public void onConnected() {
                    viewModel.onServiceConnected();
                }

                @Override
                public void onDisconnected() {
                    viewModel.onServiceDisconnected();
                }

                @Override
                public void onError(String message) {
                    viewModel.onServiceError(message);
                }
            });

            // Update state based on service connection
            if (boundService.isConnected()) {
                viewModel.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.i(TAG, "Service disconnected");
        }
    };

    // Permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.i(TAG, "SMS permission granted");
                    startGatewayService();
                } else {
                    Log.w(TAG, "SMS permission denied");
                    showError("SMS permission required to send messages");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "MainActivity created");

        // Initialize views
        initializeViews();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Set up RecyclerView
        setupEventLog();

        // Observe LiveData
        observeViewModel();

        // Check and request permissions
        checkPermissions();

        // Bind to service
        bindToService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        Log.d(TAG, "onDestroy");
    }

    /**
     * Initialize all view references.
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusChip = findViewById(R.id.statusChip);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        urlInput = findViewById(R.id.urlInput);
        deviceIdText = findViewById(R.id.deviceIdText);
        copyDeviceIdButton = findViewById(R.id.copyDeviceIdButton);
        connectButton = findViewById(R.id.connectButton);
        errorText = findViewById(R.id.errorText);
        eventLogRecycler = findViewById(R.id.eventLogRecycler);

        // Set up button click listeners
        connectButton.setOnClickListener(v -> onConnectButtonClicked());
        copyDeviceIdButton.setOnClickListener(v -> onCopyDeviceIdClicked());
    }

    /**
     * Set up RecyclerView for event log.
     */
    private void setupEventLog() {
        eventLogAdapter = new EventLogAdapter();
        eventLogRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                // Scroll to bottom when items are added
                if (eventLogAdapter.getItemCount() > 0) {
                    eventLogRecycler.scrollToPosition(eventLogAdapter.getItemCount() - 1);
                }
            }
        });
        eventLogRecycler.setAdapter(eventLogAdapter);
    }

    /**
     * Observe ViewModel LiveData.
     */
    private void observeViewModel() {
        // Connection state
        viewModel.getConnectionState().observe(this, state -> updateConnectionUI(state));

        // Error message
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            } else {
                hideError();
            }
        });

        // Gateway URL
        viewModel.getGatewayUrl().observe(this, url -> {
            if (url != null && !url.isEmpty()) {
                urlInput.setText(url);
            }
        });

        // Device ID
        viewModel.getDeviceId().observe(this, id -> {
            deviceIdText.setText(id != null ? id : "");
        });

        // Event log
        viewModel.getEventLog().observe(this, events -> {
            if (events != null) {
                eventLogAdapter.submitList(events);
                // Scroll to bottom
                if (events.size() > 0) {
                    eventLogRecycler.scrollToPosition(events.size() - 1);
                }
            }
        });
    }

    /**
     * Update UI based on connection state.
     */
    private void updateConnectionUI(MainViewModel.ConnectionState state) {
        switch (state) {
            case IDLE:
                statusChip.setText(getString(R.string.status_not_connected));
                statusChip.setChipBackgroundColorResource(android.R.color.darker_gray);
                connectButton.setText(R.string.action_connect);
                connectButton.setEnabled(true);
                urlInput.setEnabled(true);
                deviceInfoText.setText("");
                break;

            case CONNECTING:
                statusChip.setText(getString(R.string.status_connecting));
                statusChip.setChipBackgroundColorResource(android.R.color.holo_orange_light);
                connectButton.setText(R.string.action_cancel);
                connectButton.setEnabled(true);
                urlInput.setEnabled(false);
                deviceInfoText.setText("Connecting...");
                break;

            case CONNECTED:
                statusChip.setText(getString(R.string.status_connected));
                statusChip.setChipBackgroundColorResource(android.R.color.holo_green_light);
                connectButton.setText(R.string.action_disconnect);
                connectButton.setEnabled(true);
                urlInput.setEnabled(false);
                deviceInfoText.setText("Connected to " + viewModel.getGatewayUrl().getValue());
                break;

            case ERROR:
                statusChip.setText(getString(R.string.status_error));
                statusChip.setChipBackgroundColorResource(android.R.color.holo_red_light);
                connectButton.setText(R.string.action_retry);
                connectButton.setEnabled(true);
                urlInput.setEnabled(true);
                break;
        }
    }

    /**
     * Handle Connect button click.
     */
    private void onConnectButtonClicked() {
        MainViewModel.ConnectionState state = viewModel.getCurrentConnectionState();

        if (state == MainViewModel.ConnectionState.IDLE) {
            // User is trying to connect
            String url = urlInput.getText() != null ? urlInput.getText().toString() : "";
            viewModel.onConnectClicked(url);

            if (viewModel.isConnecting()) {
                startGatewayService();
            }
        } else if (state == MainViewModel.ConnectionState.CONNECTING) {
            // User cancelled
            viewModel.onDisconnectClicked();
            stopGatewayService();
        } else if (state == MainViewModel.ConnectionState.CONNECTED) {
            // User is trying to disconnect
            viewModel.onDisconnectClicked();
            stopGatewayService();
        } else if (state == MainViewModel.ConnectionState.ERROR) {
            // User is retrying
            viewModel.onConnectClicked(urlInput.getText() != null ? urlInput.getText().toString() : "");
            startGatewayService();
        }
    }

    /**
     * Handle copy device ID button click.
     */
    private void onCopyDeviceIdClicked() {
        String deviceId = deviceIdText.getText() != null ? deviceIdText.getText().toString() : "";
        if (!deviceId.isEmpty()) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Device ID", deviceId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Device ID copied", Toast.LENGTH_SHORT).show();
            viewModel.copyDeviceIdToClipboard();
        }
    }

    /**
     * Check for required permissions.
     */
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "SMS permission not granted, requesting...");
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            }
        }
    }

    /**
     * Start the gateway foreground service.
     */
    private void startGatewayService() {
        if (!viewModel.isConfigured()) {
            showError("Please enter a server URL first");
            return;
        }

        Log.i(TAG, "Starting gateway service");
        Intent serviceIntent = new Intent(this, GatewayForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /**
     * Stop the gateway foreground service.
     */
    private void stopGatewayService() {
        Log.i(TAG, "Stopping gateway service");
        Intent serviceIntent = new Intent(this, GatewayForegroundService.class);
        stopService(serviceIntent);
    }

    /**
     * Bind to the gateway service.
     */
    private void bindToService() {
        Intent serviceIntent = new Intent(this, GatewayForegroundService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Show error message.
     */
    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    /**
     * Hide error message.
     */
    private void hideError() {
        errorText.setVisibility(View.GONE);
    }
}
