import 'dart:convert';
import 'package:http/http.dart' as http;

class GameApi {
  Future<String> fetchWordOfDay(String date) async {
    final response = await http.get(
      Uri.parse('https://www.nytimes.com/svc/wordle/v2/$date.json'),
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body)['solution'] as String;
    }
    throw Exception('Failed to load word');
  }

  Future<List<String>> fetchWordList() async {
    final response = await http.get(
      Uri.parse('https://raw.githubusercontent.com/tabatkins/wordle-list/main/words'),
    );
    if (response.statusCode == 200) {
      return response.body.split('\n').where((w) => w.length == 5).map((w) => w.toLowerCase().trim()).toList();
    }
    throw Exception('Failed to load word list');
  }
}
