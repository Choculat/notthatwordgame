import 'package:flutter/material.dart';
import '../../controllers/letter_state.dart';

class GameKeyboard extends StatelessWidget {
  final void Function(String) onKey;
  final VoidCallback onEnter;
  final VoidCallback onBackspace;
  final Map<String, LetterState> letterStates;
  const GameKeyboard({super.key, required this.onKey, required this.onEnter, required this.onBackspace, required this.letterStates});

  static const _rows = ['qwertyuiop', 'asdfghjkl', 'zxcvbnm'];
  static const _correctColor = Color(0xFF538D4E);
  static const _misplacedColor = Color(0xFFB59F3B);
  static const _wrongColorDark = Color(0xFF3A3A3C);
  static const _wrongColorLight = Color(0xFF787C7E);
  static const _defaultColorDark = Color(0xFF818384);
  static const _defaultColorLight = Color(0xFFD3D6DA);

  Color _getKeyColor(String letter, bool isDark) => switch (letterStates[letter]) {
    LetterState.correct => _correctColor,
    LetterState.misplaced => _misplacedColor,
    LetterState.wrong => isDark ? _wrongColorDark : _wrongColorLight,
    _ => isDark ? _defaultColorDark : _defaultColorLight,
  };

  Widget _buildKey(String letter, bool isDark, {double width = 32, Widget? child, VoidCallback? onTap}) {
    final defaultColor = isDark ? _defaultColorDark : _defaultColorLight;
    final textColor = letterStates[letter] != null || child != null ? Colors.white : (isDark ? Colors.white : Colors.black);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 3, vertical: 4),
      child: Material(
        color: child != null ? defaultColor : _getKeyColor(letter, isDark),
        borderRadius: BorderRadius.circular(4),
        child: InkWell(
          borderRadius: BorderRadius.circular(4),
          onTap: onTap ?? () => onKey(letter),
          child: Container(
            width: width,
            height: 58,
            alignment: Alignment.center,
            child: child ?? Text(letter.toUpperCase(), style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: textColor)),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final specialKeyColor = isDark ? Colors.white : Colors.black;
    return Column(
      children: [
        Row(mainAxisAlignment: MainAxisAlignment.center, children: _rows[0].split('').map((l) => _buildKey(l, isDark)).toList()),
        Row(mainAxisAlignment: MainAxisAlignment.center, children: _rows[1].split('').map((l) => _buildKey(l, isDark)).toList()),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildKey('', isDark, width: 56, child: Text('ENTER', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: specialKeyColor)), onTap: onEnter),
            ..._rows[2].split('').map((l) => _buildKey(l, isDark)),
            _buildKey('', isDark, width: 56, child: Icon(Icons.backspace_outlined, color: specialKeyColor, size: 22), onTap: onBackspace),
          ],
        ),
      ],
    );
  }
}
