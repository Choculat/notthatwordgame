import 'package:shared_preferences/shared_preferences.dart';
import '../api/game_api.dart';

class GameService {
  static const _keyPrefix = 'game_';
  static const _wordListKey = 'word_list';
  final GameApi _api = GameApi();
  Set<String>? _wordList;

  Future<String> getWordOfDay() async {
    final today = DateTime.now().toIso8601String().split('T')[0];
    final prefs = await SharedPreferences.getInstance();
    final cached = prefs.getString('$_keyPrefix$today');
    if (cached != null) return cached;
    final word = await _api.fetchWordOfDay(today);
    await prefs.setString('$_keyPrefix$today', word);
    return word;
  }

  Future<void> loadWordList() async {
    final prefs = await SharedPreferences.getInstance();
    final cached = prefs.getStringList(_wordListKey);
    if (cached != null) {
      _wordList = cached.toSet();
      return;
    }
    final words = await _api.fetchWordList();
    await prefs.setStringList(_wordListKey, words);
    _wordList = words.toSet();
  }

  bool isValidWord(String word) => _wordList?.contains(word.toLowerCase()) ?? true;
}
