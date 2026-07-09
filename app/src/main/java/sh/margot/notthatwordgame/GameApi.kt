package sh.margot.notthatwordgame

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ponytail: blocking HttpURLConnection calls, meant to run off the main thread (see MainActivity's Thread usage)
object GameApi {
    private fun get(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode != 200) null else connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    fun fetchWordOfDay(date: String): String? {
        val body = get("https://www.nytimes.com/svc/wordle/v2/$date.json") ?: return null
        return JSONObject(body).optString("solution").ifEmpty { null }
    }

    fun fetchWordList(): List<String>? {
        val body = get("https://raw.githubusercontent.com/tabatkins/wordle-list/main/words") ?: return null
        return body.lineSequence().map { it.trim().lowercase() }.filter { it.length == 5 }.toList()
    }

    fun fetchDefinition(word: String): String? {
        val body = get("https://api.dictionaryapi.dev/api/v2/entries/en/$word") ?: return null
        return try {
            val meanings = JSONArray(body).getJSONObject(0).getJSONArray("meanings")
            if (meanings.length() == 0) return null
            val definitions = meanings.getJSONObject(0).getJSONArray("definitions")
            if (definitions.length() == 0) return null
            definitions.getJSONObject(0).optString("definition").ifEmpty { null }
        } catch (e: Exception) {
            null
        }
    }
}
