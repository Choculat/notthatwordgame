package sh.margot.notthatwordgame

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Persists to SharedPreferences, which Android backs by a file on disk, so game
// progress and history survive the app being closed.
class GameRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("game", Context.MODE_PRIVATE)

    data class Progress(
        val guesses: List<String>,
        val gameOver: Boolean,
        val hintUsed: Boolean,
        val hintVowel: Char?,
        val hintConsonant: Char?,
    )

    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun cachedWordOfDay(date: String): String? = prefs.getString("word_$date", null)
    fun cacheWordOfDay(date: String, word: String) = prefs.edit().putString("word_$date", word).apply()

    fun cachedWordList(): Set<String>? = prefs.getStringSet("word_list", null)
    fun cacheWordList(words: List<String>) = prefs.edit().putStringSet("word_list", words.toSet()).apply()

    fun saveProgress(guesses: List<String>, gameOver: Boolean, hintUsed: Boolean, hintVowel: Char?, hintConsonant: Char?) {
        val json = JSONObject()
            .put("guesses", JSONArray(guesses))
            .put("gameOver", gameOver)
            .put("hintUsed", hintUsed)
            .put("hintVowel", hintVowel?.toString())
            .put("hintConsonant", hintConsonant?.toString())
        prefs.edit().putString("progress_${todayKey()}", json.toString()).apply()
    }

    fun loadProgress(): Progress? {
        val raw = prefs.getString("progress_${todayKey()}", null) ?: return null
        val json = JSONObject(raw)
        val guessesArray = json.getJSONArray("guesses")
        val guesses = (0 until guessesArray.length()).map { guessesArray.getString(it) }
        return Progress(
            guesses,
            json.getBoolean("gameOver"),
            json.optBoolean("hintUsed", false),
            json.optString("hintVowel", null)?.firstOrNull(),
            json.optString("hintConsonant", null)?.firstOrNull(),
        )
    }

    fun getHistory(): List<HistoryEntry> {
        val raw = prefs.getStringSet("history", null) ?: return emptyList()
        return raw.map { HistoryEntry.fromJson(JSONObject(it)) }
    }

    fun addHistoryEntry(entry: HistoryEntry) {
        saveHistory(getHistory().filter { it.date != entry.date } + entry)
    }

    private fun saveHistory(entries: List<HistoryEntry>) {
        prefs.edit().putStringSet("history", entries.map { it.toJson().toString() }.toSet()).apply()
    }

    fun exportHistory(): String = JSONArray(getHistory().map { it.toJson() }).toString()

    fun importHistory(json: String) {
        val array = JSONArray(json)
        val imported = (0 until array.length()).map { HistoryEntry.fromJson(array.getJSONObject(it)) }
        val byDate = getHistory().associateBy { it.date }.toMutableMap()
        imported.forEach { byDate[it.date] = it }
        saveHistory(byDate.values.toList())
    }

    // "auto" follows the system theme, "light"/"dark" force it. Applied in-place by the
    // activities (no recreate()) so toggling never flashes or re-lays-out the screen.
    fun themeOverride(): String = prefs.getString("theme_mode", "auto") ?: "auto"
    fun setThemeOverride(mode: String) = prefs.edit().putString("theme_mode", mode).apply()
}
