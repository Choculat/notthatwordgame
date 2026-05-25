import 'package:flutter/material.dart';
import '../../controllers/letter_state.dart';

class LetterBox extends StatelessWidget {
  final String letter;
  final LetterState state;
  final bool hasLetter;
  final double offsetX;
  final double offsetY;
  const LetterBox({super.key, required this.letter, required this.state, this.hasLetter = false, this.offsetX = 0, this.offsetY = 0});

  static const correctColor = Color(0xFF538D4E);
  static const misplacedColor = Color(0xFFB59F3B);
  static const wrongColorDark = Color(0xFF3A3A3C);
  static const wrongColorLight = Color(0xFF787C7E);

  Color _getColor(bool isDark) => switch (state) {
    LetterState.correct => correctColor,
    LetterState.misplaced => misplacedColor,
    LetterState.wrong => isDark ? wrongColorDark : wrongColorLight,
    LetterState.empty => Colors.transparent,
  };

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final borderColor = isDark ? const Color(0xFF3A3A3C) : Colors.grey.shade400;
    final activeBorderColor = isDark ? const Color(0xFF565758) : Colors.grey.shade700;
    final textColor = state == LetterState.empty ? (isDark ? Colors.white : Colors.black) : Colors.white;

    return Transform.translate(
      offset: Offset(offsetX, offsetY),
      child: Container(
        width: 56,
        height: 56,
        margin: const EdgeInsets.all(3),
        decoration: BoxDecoration(
          color: _getColor(isDark),
          border: Border.all(
            color: state == LetterState.empty ? (hasLetter ? activeBorderColor : borderColor) : Colors.transparent,
            width: 2,
          ),
        ),
        child: Center(
          child: Text(letter.toUpperCase(), style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: textColor)),
        ),
      ),
    );
  }
}
