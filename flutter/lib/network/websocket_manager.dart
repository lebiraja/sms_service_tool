import 'package:web_socket_channel/web_socket_channel.dart';
import 'dart:async';
import 'message_parser.dart';

enum ConnectionState {
  idle,
  connecting,
  connected,
  error,
}

typedef OnStateChange = void Function(ConnectionState state, String? errorMsg);

class WebSocketManager {
  WebSocketChannel? _channel;
  ConnectionState _state = ConnectionState.idle;
  String? _lastError;
  OnStateChange? onStateChange;
  final Duration retryDelay;
  final int maxRetryDelaySeconds;
  int _retryAttempt = 0;
  Timer? _reconnectTimer;
  StreamSubscription? _subscription;

  WebSocketManager({
    this.onStateChange,
    this.retryDelay = const Duration(seconds: 5),
    this.maxRetryDelaySeconds = 60,
  });

  ConnectionState get state => _state;
  String? get lastError => _lastError;

  Future<void> connect(String url) async {
    if (_state == ConnectionState.connecting || _state == ConnectionState.connected) {
      return;
    }

    _setState(ConnectionState.connecting);
    _lastError = null;

    try {
      final normalizedUrl = _normalizeUrl(url);
      // Debug: log the normalized URL
      print('WebSocket connecting to: $normalizedUrl');

      _channel = WebSocketChannel.connect(Uri.parse(normalizedUrl));

      // Wait for connection to establish with timeout
      await _channel!.ready.timeout(
        const Duration(seconds: 10),
        onTimeout: () {
          throw Exception('WebSocket connection timeout (10s) - is the server reachable?');
        },
      );

      _setState(ConnectionState.connected);
      _retryAttempt = 0;

      // Listen for messages
      _subscribeToMessages();
    } catch (e) {
      _lastError = e.toString();
      _setState(ConnectionState.error);
      _scheduleReconnect();
    }
  }

  void _subscribeToMessages() {
    _subscription?.cancel();
    _subscription = _channel?.stream.listen(
      (message) {
        // Handle server message (will be forwarded to handlers)
      },
      onError: (error) {
        _lastError = error.toString();
        _setState(ConnectionState.error);
        _scheduleReconnect();
      },
      onDone: () {
        _setState(ConnectionState.error);
        _scheduleReconnect();
      },
    );
  }

  void _scheduleReconnect() {
    _reconnectTimer?.cancel();
    final delaySeconds = (retryDelay.inSeconds *
            (1 << _retryAttempt)) // 5s * 2^attempt
        .clamp(0, maxRetryDelaySeconds);
    _retryAttempt++;

    _reconnectTimer = Timer(Duration(seconds: delaySeconds), () {
      // Will be called by the foreground task
    });
  }

  void send(String message) {
    try {
      _channel?.sink.add(message);
    } catch (e) {
      _lastError = e.toString();
      _setState(ConnectionState.error);
    }
  }

  Stream<ServerMessage> get messageStream async* {
    if (_channel == null) return;
    yield* _channel!.stream
        .map((msg) {
          try {
            return MessageParser.parseServerMessage(msg as String);
          } catch (e) {
            return null;
          }
        })
        .where((msg) => msg != null)
        .cast<ServerMessage>();
  }

  void disconnect() {
    _reconnectTimer?.cancel();
    _subscription?.cancel();
    _channel?.sink.close();
    _channel = null;
    _setState(ConnectionState.idle);
    _retryAttempt = 0;
  }

  void _setState(ConnectionState newState) {
    if (_state != newState) {
      _state = newState;
      onStateChange?.call(newState, _lastError);
    }
  }

  String _normalizeUrl(String url) {
    url = url.trim();

    // Remove http:// or https:// if present (convert to ws://)
    if (url.startsWith('https://')) {
      url = url.replaceFirst('https://', 'wss://');
    } else if (url.startsWith('http://')) {
      url = url.replaceFirst('http://', 'ws://');
    }

    // Add ws:// prefix if no protocol specified
    if (!url.startsWith('ws://') && !url.startsWith('wss://')) {
      url = 'ws://$url';
    }

    // Add /ws suffix if path is missing
    if (!url.contains('/ws')) {
      if (!url.endsWith('/')) {
        url += '/';
      }
      url += 'ws';
    }

    return url;
  }

  void dispose() {
    disconnect();
  }
}
