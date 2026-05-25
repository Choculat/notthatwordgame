import 'package:flutter/material.dart';
import 'dart:math';
import '../../controllers/letter_state.dart';

class FlipCard extends StatefulWidget {
  final String letter;
  final LetterState state;
  final Duration delay;
  final bool animate;
  const FlipCard({super.key, required this.letter, required this.state, this.delay = Duration.zero, this.animate = false});
  @override
  State<FlipCard> createState() => _FlipCardState();
}

class _FlipCardState extends State<FlipCard> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  static const correctColor = Color(0xFF538D4E);
  static const misplacedColor = Color(0xFFB59F3B);
  static const wrongColorDark = Color(0xFF3A3A3C);
  static const wrongColorLight = Color(0xFF787C7E);

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(vsync: this, duration: const Duration(milliseconds: 500));
    _animation = Tween<double>(begin: 0, end: 1).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));
    if (widget.animate) Future.delayed(widget.delay, () => mounted ? _controller.forward() : null);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Color _getColor(LetterState state, bool isDark) => switch (state) {
    LetterState.correct => correctColor,
    LetterState.misplaced => misplacedColor,
    LetterState.wrong => isDark ? wrongColorDark : wrongColorLight,
    LetterState.empty => Colors.transparent,
  };

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final borderColor = isDark ? const Color(0xFF3A3A3C) : Colors.grey.shade400;
    final textColorBefore = isDark ? Colors.white : Colors.black;

    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        final angle = _animation.value * pi;
        final showBack = _animation.value >= 0.5;
        return Transform(
          alignment: Alignment.center,
          transform: Matrix4.identity()..setEntry(3, 2, 0.001)..rotateX(angle),
          child: Container(
            width: 56,
            height: 56,
            margin: const EdgeInsets.all(3),
            decoration: BoxDecoration(
              color: showBack ? _getColor(widget.state, isDark) : Colors.transparent,
              border: Border.all(color: showBack ? Colors.transparent : borderColor, width: 2),
            ),
            child: Center(
              child: Transform(
                alignment: Alignment.center,
                transform: Matrix4.identity()..rotateX(showBack ? pi : 0),
                child: Text(widget.letter.toUpperCase(), style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: showBack ? Colors.white : textColorBefore)),
              ),
            ),
          ),
        );
      },
    );
  }
}
