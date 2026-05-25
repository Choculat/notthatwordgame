import 'package:flutter/material.dart';
import 'controllers/theme_controller.dart';
import 'pages/game_page.dart';

final themeController = ThemeController();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await themeController.loadTheme();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});
  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    themeController.addListener(_onThemeChange);
  }

  @override
  void dispose() {
    themeController.removeListener(_onThemeChange);
    super.dispose();
  }

  void _onThemeChange() => setState(() {});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Word Game',
      debugShowCheckedModeBanner: false,
      themeMode: themeController.themeMode,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green, brightness: Brightness.light),
        useMaterial3: true,
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green, brightness: Brightness.dark),
        useMaterial3: true,
      ),
      home: const GamePage(),
    );
  }
}

