import 'package:flutter/material.dart';
import '../controllers/game_controller.dart';
import '../main.dart';
import '../widgets/game_grid.dart';
import '../widgets/game_keyboard.dart';
import '../widgets/error_view.dart';
import '../widgets/loading_view.dart';

class GamePage extends StatefulWidget {
  const GamePage({super.key});
  @override
  State<GamePage> createState() => _GamePageState();
}

class _GamePageState extends State<GamePage> {
  final _controller = GameController();

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onStateChange);
    themeController.addListener(_onStateChange);
    _controller.loadData();
  }

  @override
  void dispose() {
    _controller.removeListener(_onStateChange);
    themeController.removeListener(_onStateChange);
    _controller.dispose();
    super.dispose();
  }

  void _onStateChange() => setState(() {});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final bgColor = isDark ? const Color(0xFF121213) : Colors.white;
    final borderColor = isDark ? const Color(0xFF3A3A3C) : Colors.grey.shade300;
    final titleColor = isDark ? Colors.white : Colors.black;

    if (_controller.loadError) return ErrorView(onRetry: _controller.loadData);
    if (_controller.loading) return const LoadingView();
    return Scaffold(
      backgroundColor: bgColor,
      appBar: AppBar(
        backgroundColor: bgColor,
        title: Text('Not that Word Game', style: TextStyle(color: titleColor, fontWeight: FontWeight.bold, fontSize: 28)),
        centerTitle: true,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(themeController.icon, color: titleColor),
            onPressed: themeController.cycleTheme,
          ),
        ],
        bottom: PreferredSize(preferredSize: const Size.fromHeight(1), child: Container(color: borderColor, height: 1)),
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          const gridHeight = 6 * 62.0;
          final spaceAboveGrid = (constraints.maxHeight - gridHeight - 200) / 2;
          return Stack(
            children: [
              Column(
                children: [
                  Expanded(
                    child: Center(
                      child: GameGrid(
                        guesses: _controller.guesses,
                        currentGuess: _controller.currentGuess,
                        solution: _controller.solution,
                        shaking: _controller.shaking,
                        flippingRow: _controller.flippingRow,
                        jumpingRow: _controller.jumpingRow,
                      ),
                    ),
                  ),
                  GameKeyboard(
                    onKey: _controller.onKey,
                    onEnter: _controller.onEnter,
                    onBackspace: _controller.onBackspace,
                    letterStates: _controller.letterStates,
                  ),
                  SizedBox(height: MediaQuery.of(context).padding.bottom + 24),
                ],
              ),
              if (_controller.message.isNotEmpty) Positioned(
                top: spaceAboveGrid / 2 - 20,
                left: 0,
                right: 0,
                child: Center(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(4)),
                    child: Text(_controller.message, style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 14)),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
