import 'package:flutter/material.dart';
import '../services/game_service.dart';
import 'letter_state.dart';

class GameController extends ChangeNotifier {
  final GameService _service = GameService();
  String _solution = '';
  final List<String> _guesses = [];
  String _currentGuess = '';
  bool _gameOver = false;
  String _message = '';
  bool _shaking = false;
  int? _flippingRow;
  int? _jumpingRow;
  bool _loadError = false;
  bool _loading = true;

  String get solution => _solution;
  List<String> get guesses => _guesses;
  String get currentGuess => _currentGuess;
  bool get gameOver => _gameOver;
  String get message => _message;
  bool get shaking => _shaking;
  int? get flippingRow => _flippingRow;
  int? get jumpingRow => _jumpingRow;
  bool get loadError => _loadError;
  bool get loading => _loading;

  Map<String, LetterState> get letterStates {
    final states = <String, LetterState>{};
    final revealedCount = _flippingRow ?? _guesses.length;
    for (var g = 0; g < revealedCount; g++) {
      final guess = _guesses[g];
      for (var i = 0; i < 5; i++) {
        final letter = guess[i].toLowerCase();
        final current = states[letter] ?? LetterState.empty;
        if (guess[i].toLowerCase() == _solution[i].toLowerCase()) {
          states[letter] = LetterState.correct;
        } else if (_solution.toLowerCase().contains(letter) && current != LetterState.correct) {
          states[letter] = LetterState.misplaced;
        } else if (current == LetterState.empty) {
          states[letter] = LetterState.wrong;
        }
      }
    }
    return states;
  }

  Future<void> loadData() async {
    _loadError = false;
    _loading = true;
    notifyListeners();
    try {
      await _service.loadWordList();
      _solution = await _service.getWordOfDay();
      _loading = false;
    } catch (e) {
      _loadError = true;
      _loading = false;
    }
    notifyListeners();
  }

  void onKey(String letter) {
    if (_gameOver || _currentGuess.length >= 5 || _flippingRow != null) return;
    _currentGuess += letter;
    _message = '';
    notifyListeners();
  }

  void onBackspace() {
    if (_currentGuess.isEmpty || _flippingRow != null) return;
    _currentGuess = _currentGuess.substring(0, _currentGuess.length - 1);
    _message = '';
    notifyListeners();
  }

  void onEnter() {
    if (_currentGuess.length != 5 || _gameOver || _flippingRow != null) return;
    if (!_service.isValidWord(_currentGuess)) {
      _message = 'Not in word list';
      _shaking = true;
      notifyListeners();
      Future.delayed(const Duration(milliseconds: 600), () {
        _shaking = false;
        notifyListeners();
      });
      return;
    }
    final isCorrect = _currentGuess.toLowerCase() == _solution.toLowerCase();
    final rowIndex = _guesses.length;
    _guesses.add(_currentGuess);
    _currentGuess = '';
    _flippingRow = rowIndex;
    notifyListeners();
    Future.delayed(const Duration(milliseconds: 1750), () {
      _flippingRow = null;
      if (isCorrect) {
        _gameOver = true;
        _jumpingRow = rowIndex;
        _message = 'Splendid!';
      } else if (_guesses.length >= 6) {
        _gameOver = true;
        _message = _solution.toUpperCase();
      }
      notifyListeners();
    });
  }
}
