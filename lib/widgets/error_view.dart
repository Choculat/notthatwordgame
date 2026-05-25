import 'package:flutter/material.dart';

class ErrorView extends StatelessWidget {
  final VoidCallback onRetry;
  const ErrorView({super.key, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bgColor = isDark ? const Color(0xFF121213) : Colors.white;
    final textColor = isDark ? Colors.white : Colors.black;

    return Scaffold(
      backgroundColor: bgColor,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.wifi_off, color: textColor, size: 64),
            const SizedBox(height: 16),
            Text('No internet connection', style: TextStyle(color: textColor, fontSize: 18)),
            const SizedBox(height: 24),
            FilledButton(onPressed: onRetry, child: const Text('Retry')),
          ],
        ),
      ),
    );
  }
}
