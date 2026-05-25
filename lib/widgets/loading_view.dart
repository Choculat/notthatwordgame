import 'package:flutter/material.dart';

class LoadingView extends StatelessWidget {
  const LoadingView({super.key});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bgColor = isDark ? const Color(0xFF121213) : Colors.white;

    return Scaffold(
      backgroundColor: bgColor,
      body: const Center(child: CircularProgressIndicator()),
    );
  }
}
