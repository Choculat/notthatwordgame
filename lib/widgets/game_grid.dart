import 'package:flutter/material.dart';
import '../../controllers/letter_state.dart';
import 'letter_box.dart';
import 'flip_card.dart';

class GameGrid extends StatefulWidget {
  final List<String> guesses;
  final String currentGuess;
  final String solution;
  final int maxGuesses;
  final bool shaking;
  final int? flippingRow;
  final int? jumpingRow;
  const GameGrid({super.key, required this.guesses, required this.currentGuess, required this.solution, this.maxGuesses = 6, this.shaking = false, this.flippingRow, this.jumpingRow});
  @override
  State<GameGrid> createState() => _GameGridState();
}

class _GameGridState extends State<GameGrid> with TickerProviderStateMixin {
  late AnimationController _shakeController;
  late Animation<double> _shakeAnimation;
  late AnimationController _jumpController;
  late List<Animation<double>> _jumpAnimations;

  @override
  void initState() {
    super.initState();
    _shakeController = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    _shakeAnimation = TweenSequence<double>([
      TweenSequenceItem(tween: Tween(begin: 0, end: -5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: -5, end: 5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: 5, end: -5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: -5, end: 5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: 5, end: -5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: -5, end: 5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: 5, end: -5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: -5, end: 5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: 5, end: -5), weight: 1),
      TweenSequenceItem(tween: Tween(begin: -5, end: 0), weight: 1),
    ]).animate(_shakeController);
    _jumpController = AnimationController(vsync: this, duration: const Duration(milliseconds: 1500));
    _jumpAnimations = List.generate(5, (i) {
      final start = i * 0.1;
      final mid = start + 0.1;
      final end = mid + 0.1;
      return TweenSequence<double>([
        TweenSequenceItem(tween: Tween(begin: 0, end: -30), weight: mid - start),
        TweenSequenceItem(tween: Tween(begin: -30, end: 0), weight: end - mid),
        TweenSequenceItem(tween: ConstantTween(0), weight: 1 - end),
      ]).animate(CurvedAnimation(parent: _jumpController, curve: Interval(start, 1)));
    });
  }

  @override
  void didUpdateWidget(GameGrid oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.shaking && !oldWidget.shaking) _shakeController.forward(from: 0);
    if (widget.jumpingRow != null && oldWidget.jumpingRow == null) _jumpController.forward(from: 0);
  }

  @override
  void dispose() {
    _shakeController.dispose();
    _jumpController.dispose();
    super.dispose();
  }

  List<LetterState> _getStates(String guess) {
    final states = List.filled(5, LetterState.wrong);
    final solutionChars = widget.solution.toLowerCase().split('');
    final guessChars = guess.toLowerCase().split('');
    for (var i = 0; i < 5; i++) {
      if (guessChars[i] == solutionChars[i]) {
        states[i] = LetterState.correct;
        solutionChars[i] = '';
      }
    }
    for (var i = 0; i < 5; i++) {
      if (states[i] != LetterState.correct && solutionChars.contains(guessChars[i])) {
        states[i] = LetterState.misplaced;
        solutionChars[solutionChars.indexOf(guessChars[i])] = '';
      }
    }
    return states;
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: Listenable.merge([_shakeAnimation, _jumpController]),
      builder: (context, child) {
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: List.generate(widget.maxGuesses, (row) {
            final isCurrentRow = row == widget.guesses.length;
            final isFlipping = widget.flippingRow == row;
            final isJumping = widget.jumpingRow == row;
            final guess = row < widget.guesses.length ? widget.guesses[row] : (isCurrentRow ? widget.currentGuess.padRight(5) : '     ');
            final states = row < widget.guesses.length ? _getStates(guess) : List.filled(5, LetterState.empty);
            final shakeOffset = isCurrentRow && widget.shaking ? _shakeAnimation.value : 0.0;
            return Transform.translate(
              offset: Offset(shakeOffset, 0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: List.generate(5, (col) {
                  if (isFlipping) {
                    return FlipCard(
                      letter: guess.length > col ? guess[col] : '',
                      state: states[col],
                      delay: Duration(milliseconds: col * 250),
                      animate: true,
                    );
                  }
                  final jumpOffset = isJumping ? _jumpAnimations[col].value : 0.0;
                  return LetterBox(
                    letter: guess.length > col ? guess[col] : '',
                    state: states[col],
                    hasLetter: isCurrentRow && col < widget.currentGuess.length,
                    offsetY: jumpOffset,
                  );
                }),
              ),
            );
          }),
        );
      },
    );
  }
}
