package sh.margot.notthatwordgame

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.min
import kotlin.random.Random
import sh.margot.notthatwordgame.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val MAX_GUESSES = 6
        private const val WORD_LENGTH = 5
        private const val VOWELS = "aeiou"
        private val WIN_MESSAGES = listOf("Genius", "Magnificent", "Impressive", "Splendid", "Great", "Phew")
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: GameRepository

    private lateinit var tiles: Array<Array<TextView>>
    private lateinit var rowViews: Array<LinearLayout>
    private lateinit var enterKey: TextView
    private lateinit var backspaceKey: TextView
    private val keyViews = mutableMapOf<Char, TextView>()

    private var themeMode = "auto"
    private var solution = ""
    private var wordSet: Set<String>? = null
    private val guesses = mutableListOf<String>()
    private var currentGuess = ""
    private var gameOver = false
    private var animating = false
    private var hintUsed = false
    private var hintVowel: Char? = null
    private var hintConsonant: Char? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hideMessageRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = GameRepository(this)
        themeMode = repo.themeOverride()

        buildGrid()
        buildKeyboard()
        binding.retryButton.setOnClickListener { loadGame() }
        binding.hintButton.setOnClickListener { onHint() }
        binding.historyButton.setOnClickListener { startActivity(android.content.Intent(this, HistoryActivity::class.java)) }
        binding.themeButton.setOnClickListener { cycleTheme() }
        refreshTheme()
        loadGame()
    }

    // region Setup

    private fun isDark(): Boolean = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildGrid() {
        rowViews = Array(MAX_GUESSES) { LinearLayout(this) }
        tiles = Array(MAX_GUESSES) { row ->
            val rowLayout = rowViews[row].apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            binding.gridContainer.addView(rowLayout)
            Array(WORD_LENGTH) { buildTile(rowLayout) }
        }
    }

    private fun buildTile(parent: LinearLayout): TextView {
        val tile = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 28f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = tileDrawable(null)
        }
        val size = dp(58)
        val params = LinearLayout.LayoutParams(size, size).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        parent.addView(tile, params)
        return tile
    }

    private fun tileDrawable(fillColor: Int?): GradientDrawable = GradientDrawable().apply {
        if (fillColor != null) {
            setColor(fillColor)
        } else {
            setColor(Color.TRANSPARENT)
            setStroke(dp(2), ContextCompat.getColor(this@MainActivity, if (isDark()) R.color.border_dark else R.color.border_light))
        }
    }

    private fun buildKeyboard() {
        val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
        rows.forEachIndexed { index, row ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            binding.keyboardContainer.addView(rowLayout)
            if (index == 2) enterKey = buildSpecialKey(rowLayout, "ENTER") { onEnter() }
            row.forEach { letter -> buildKey(rowLayout, letter) }
            if (index == 2) backspaceKey = buildSpecialKey(rowLayout, "⌫") { onBackspace() }
        }
    }

    private fun buildKey(parent: LinearLayout, letter: Char) {
        val key = TextView(this).apply {
            text = letter.uppercase()
            gravity = Gravity.CENTER
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            isClickable = true
            isFocusable = true
            setOnClickListener { onKey(letter) }
        }
        parent.addView(key, LinearLayout.LayoutParams(dp(32), dp(58)).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
        keyViews[letter] = key
        applyKeyColor(key, LetterState.EMPTY)
    }

    private fun buildSpecialKey(parent: LinearLayout, label: String, onClick: () -> Unit): TextView {
        val key = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        parent.addView(key, LinearLayout.LayoutParams(dp(50), dp(58)).apply { setMargins(dp(3), dp(4), dp(3), dp(4)) })
        return key
    }

    private fun keyDrawable(color: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(4).toFloat()
    }

    // endregion

    // region Loading

    private fun loadGame() {
        binding.errorView.visibility = View.GONE
        binding.loadingSpinner.visibility = View.VISIBLE
        Thread {
            val date = repo.todayKey()
            val fetchedSolution = repo.cachedWordOfDay(date) ?: GameApi.fetchWordOfDay(date)?.also { repo.cacheWordOfDay(date, it) }
            val fetchedWords = repo.cachedWordList() ?: GameApi.fetchWordList()?.also { repo.cacheWordList(it) }?.toSet()
            runOnUiThread {
                binding.loadingSpinner.visibility = View.GONE
                if (fetchedSolution == null || fetchedWords == null) {
                    binding.errorView.visibility = View.VISIBLE
                } else {
                    solution = fetchedSolution
                    wordSet = fetchedWords
                    restoreProgress()
                }
            }
        }.start()
    }

    private fun restoreProgress() {
        val progress = repo.loadProgress() ?: return
        progress.guesses.forEachIndexed { row, guess ->
            guesses.add(guess)
            renderRowStatic(row, guess)
        }
        gameOver = progress.gameOver
        hintUsed = progress.hintUsed
        hintVowel = progress.hintVowel
        hintConsonant = progress.hintConsonant
        updateKeyboardColors()
        if (gameOver) {
            val won = guesses.isNotEmpty() && guesses.last().equals(solution, ignoreCase = true)
            val result = if (won) "won" else "lost"
            binding.gridContainer.visibility = View.GONE
            binding.keyboardContainer.visibility = View.GONE
            binding.alreadyPlayedText.apply {
                text = "You already played today.\nThe word was ${solution.uppercase()}. You $result.\nCome back tomorrow!"
                setTextColor(if (isDark()) Color.WHITE else Color.BLACK)
                visibility = View.VISIBLE
            }
        }
    }

    // endregion

    // region Gameplay

    private fun onKey(letter: Char) {
        if (gameOver || currentGuess.length >= WORD_LENGTH || animating) return
        currentGuess += letter
        renderCurrentRow()
        hideMessage()
    }

    private fun onBackspace() {
        if (currentGuess.isEmpty() || animating) return
        currentGuess = currentGuess.dropLast(1)
        renderCurrentRow()
        hideMessage()
    }

    private fun onHint() {
        if (gameOver || solution.isEmpty()) return
        if (!hintUsed) {
            val letters = solution.lowercase().toSet()
            hintVowel = letters.filter { it in VOWELS }.randomOrNull()
            hintConsonant = letters.filter { it !in VOWELS }.randomOrNull()
            hintUsed = true
            repo.saveProgress(guesses, gameOver, hintUsed, hintVowel, hintConsonant)
        }
        val parts = listOfNotNull(hintVowel?.let { "contains ${it.uppercaseChar()}" }, hintConsonant?.let { "contains ${it.uppercaseChar()}" })
        showMessage("Hint: ${parts.joinToString(" and ")}")
    }

    private fun onEnter() {
        if (gameOver || animating) return
        if (currentGuess.length < WORD_LENGTH) {
            showMessage("Not enough letters")
            shakeRow(guesses.size)
            return
        }
        if (wordSet?.contains(currentGuess.lowercase()) == false) {
            showMessage("Not in word list")
            shakeRow(guesses.size)
            return
        }
        val row = guesses.size
        val guess = currentGuess
        val states = computeStates(guess, solution)
        guesses.add(guess)
        currentGuess = ""
        animating = true
        repo.saveProgress(guesses, false, hintUsed, hintVowel, hintConsonant)
        revealRow(row, guess, states) {
            animating = false
            updateKeyboardColors()
            val isCorrect = guess.equals(solution, ignoreCase = true)
            if (isCorrect || guesses.size >= MAX_GUESSES) {
                gameOver = true
                showMessage(winOrLoseMessage(isCorrect, row), autoHide = false)
                repo.saveProgress(guesses, true, hintUsed, hintVowel, hintConsonant)
                repo.addHistoryEntry(HistoryEntry(repo.todayKey(), solution, guesses.toList(), isCorrect, hintUsed))
                if (isCorrect) bounceRow(row)
            }
        }
    }

    private fun winOrLoseMessage(won: Boolean, row: Int): String =
        if (won) WIN_MESSAGES[min(row, WIN_MESSAGES.size - 1)] else solution.uppercase()

    // endregion

    // region Rendering

    private fun computeStates(guess: String, solution: String): List<LetterState> {
        val states = MutableList(WORD_LENGTH) { LetterState.WRONG }
        val solutionChars = solution.lowercase().toCharArray()
        val guessChars = guess.lowercase().toCharArray()
        for (i in 0 until WORD_LENGTH) {
            if (guessChars[i] == solutionChars[i]) {
                states[i] = LetterState.CORRECT
                solutionChars[i] = ' '
            }
        }
        for (i in 0 until WORD_LENGTH) {
            if (states[i] != LetterState.CORRECT) {
                val idx = solutionChars.indexOf(guessChars[i])
                if (idx >= 0) {
                    states[i] = LetterState.MISPLACED
                    solutionChars[idx] = ' '
                }
            }
        }
        return states
    }

    private fun renderCurrentRow() {
        val row = guesses.size
        if (row >= MAX_GUESSES) return
        for (col in 0 until WORD_LENGTH) {
            tiles[row][col].text = if (col < currentGuess.length) currentGuess[col].uppercase() else ""
        }
    }

    private fun renderRowStatic(row: Int, guess: String) {
        val states = computeStates(guess, solution)
        for (col in 0 until WORD_LENGTH) {
            val tile = tiles[row][col]
            tile.text = guess[col].uppercase()
            applyTileColors(tile, states[col])
        }
    }

    private fun applyTileColors(tile: TextView, state: LetterState) {
        val color = when (state) {
            LetterState.CORRECT -> ContextCompat.getColor(this, R.color.tile_correct)
            LetterState.MISPLACED -> ContextCompat.getColor(this, R.color.tile_misplaced)
            LetterState.WRONG -> ContextCompat.getColor(this, if (isDark()) R.color.tile_wrong_dark else R.color.tile_wrong_light)
            LetterState.EMPTY -> null
        }
        tile.background = tileDrawable(color)
        tile.setTextColor(if (color != null) Color.WHITE else if (isDark()) Color.WHITE else Color.BLACK)
    }

    private fun applyKeyColor(key: TextView, state: LetterState) {
        val color = when (state) {
            LetterState.CORRECT -> ContextCompat.getColor(this, R.color.tile_correct)
            LetterState.MISPLACED -> ContextCompat.getColor(this, R.color.tile_misplaced)
            LetterState.WRONG -> ContextCompat.getColor(this, if (isDark()) R.color.tile_wrong_dark else R.color.tile_wrong_light)
            LetterState.EMPTY -> ContextCompat.getColor(this, if (isDark()) R.color.key_default_dark else R.color.key_default_light)
        }
        key.background = keyDrawable(color)
        key.setTextColor(if (state == LetterState.EMPTY) (if (isDark()) Color.WHITE else Color.BLACK) else Color.WHITE)
    }

    private fun keyStates(): Map<Char, LetterState> {
        val states = mutableMapOf<Char, LetterState>()
        for (guess in guesses) {
            val rowStates = computeStates(guess, solution)
            for (i in 0 until WORD_LENGTH) {
                val letter = guess[i].lowercaseChar()
                val current = states[letter]
                states[letter] = when {
                    rowStates[i] == LetterState.CORRECT -> LetterState.CORRECT
                    rowStates[i] == LetterState.MISPLACED && current != LetterState.CORRECT -> LetterState.MISPLACED
                    current == null -> LetterState.WRONG
                    else -> current
                }
            }
        }
        return states
    }

    private fun updateKeyboardColors() {
        val states = keyStates()
        keyViews.forEach { (letter, view) -> applyKeyColor(view, states[letter] ?: LetterState.EMPTY) }
    }

    private fun showMessage(text: String, autoHide: Boolean = true) {
        hideMessageRunnable?.let { mainHandler.removeCallbacks(it) }
        binding.messageBanner.text = text
        binding.messageBanner.visibility = View.VISIBLE
        if (autoHide) {
            val runnable = Runnable { binding.messageBanner.visibility = View.GONE }
            hideMessageRunnable = runnable
            mainHandler.postDelayed(runnable, 1600)
        }
    }

    private fun hideMessage() {
        hideMessageRunnable?.let { mainHandler.removeCallbacks(it) }
        binding.messageBanner.visibility = View.GONE
    }

    // endregion

    // region Animations

    private fun revealRow(row: Int, guess: String, states: List<LetterState>, onComplete: () -> Unit) {
        for (col in 0 until WORD_LENGTH) {
            tiles[row][col].postDelayed({ flipTile(tiles[row][col], guess[col], states[col]) }, col * 250L)
        }
        binding.root.postDelayed({ onComplete() }, 1750L)
    }

    private fun flipTile(tile: TextView, letter: Char, state: LetterState) {
        val shrink = ObjectAnimator.ofFloat(tile, "scaleY", 1f, 0f).setDuration(150)
        shrink.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                tile.text = letter.uppercase()
                applyTileColors(tile, state)
                ObjectAnimator.ofFloat(tile, "scaleY", 0f, 1f).setDuration(150).start()
            }
        })
        shrink.start()
    }

    private fun shakeRow(row: Int) {
        if (row >= MAX_GUESSES) return
        ObjectAnimator.ofFloat(rowViews[row], "translationX", 0f, -10f, 10f, -10f, 10f, -6f, 6f, -2f, 2f, 0f).apply {
            duration = 600
            start()
        }
    }

    private fun bounceRow(row: Int) {
        tiles[row].forEachIndexed { col, tile ->
            tile.postDelayed({
                ObjectAnimator.ofFloat(tile, "translationY", 0f, -18f, 0f).apply { duration = 350; start() }
            }, col * 100L)
        }
    }

    // endregion

    // region Theme

    private fun cycleTheme() {
        themeMode = when (themeMode) {
            "auto" -> "light"
            "light" -> "dark"
            else -> "auto"
        }
        repo.setThemeOverride(themeMode)
        refreshTheme()
    }

    // Applies the current theme to every already-built view in place, no recreate() - that
    // was causing a visible flash and a re-layout "rescale" pop on every toggle.
    private fun refreshTheme() {
        val bgColor = ContextCompat.getColor(this, if (isDark()) R.color.bg_dark else R.color.bg_light)
        val textColor = if (isDark()) Color.WHITE else Color.BLACK
        val iconTint = ColorStateList.valueOf(textColor)
        val borderColor = ContextCompat.getColor(this, if (isDark()) R.color.border_dark else R.color.border_light)

        window.statusBarColor = bgColor
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = !isDark()
        binding.root.setBackgroundColor(bgColor)
        binding.topBar.setBackgroundColor(bgColor)
        binding.topDivider.setBackgroundColor(borderColor)

        listOf<ImageButton>(binding.hintButton, binding.historyButton).forEach { it.imageTintList = iconTint }
        updateThemeIcon()
        binding.alreadyPlayedText.setTextColor(textColor)

        val keyDefault = ContextCompat.getColor(this, if (isDark()) R.color.key_default_dark else R.color.key_default_light)
        listOf(enterKey, backspaceKey).forEach {
            it.background = keyDrawable(keyDefault)
            it.setTextColor(textColor)
        }

        val states = keyStates()
        keyViews.forEach { (letter, view) -> applyKeyColor(view, states[letter] ?: LetterState.EMPTY) }

        val emptyState = List(WORD_LENGTH) { LetterState.EMPTY }
        for (row in 0 until MAX_GUESSES) {
            val rowStates = if (row < guesses.size) computeStates(guesses[row], solution) else emptyState
            for (col in 0 until WORD_LENGTH) applyTileColors(tiles[row][col], rowStates[col])
        }
    }

    private fun updateThemeIcon() {
        val icon = when (themeMode) {
            "light" -> R.drawable.ic_theme_light
            "dark" -> R.drawable.ic_theme_dark
            else -> R.drawable.ic_theme_auto
        }
        binding.themeButton.setImageResource(icon)
        binding.themeButton.imageTintList = ColorStateList.valueOf(if (isDark()) Color.WHITE else Color.BLACK)
    }

    // endregion
}
