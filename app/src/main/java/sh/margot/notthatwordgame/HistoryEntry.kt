package sh.margot.notthatwordgame

import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val date: String, val solution: String, val guesses: List<String>, val won: Boolean, val hintUsed: Boolean = false) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("solution", solution)
        put("guesses", JSONArray(guesses))
        put("won", won)
        put("hintUsed", hintUsed)
    }

    companion object {
        fun fromJson(json: JSONObject): HistoryEntry {
            val guessesArray = json.getJSONArray("guesses")
            val guesses = (0 until guessesArray.length()).map { guessesArray.getString(it) }
            return HistoryEntry(
                json.getString("date"),
                json.getString("solution"),
                guesses,
                json.getBoolean("won"),
                json.optBoolean("hintUsed", false),
            )
        }
    }
}
