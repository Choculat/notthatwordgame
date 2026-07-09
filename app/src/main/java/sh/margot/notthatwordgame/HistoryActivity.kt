package sh.margot.notthatwordgame

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import sh.margot.notthatwordgame.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repo: GameRepository
    private var history: List<HistoryEntry> = emptyList()

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        contentResolver.openOutputStream(uri)?.use { it.write(repo.exportHistory().toByteArray()) }
        Toast.makeText(this, "History saved", Toast.LENGTH_SHORT).show()
    }

    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@registerForActivityResult
        try {
            repo.importHistory(json)
            reload()
            Toast.makeText(this, "History imported", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid history file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = GameRepository(this)
        binding.backButton.setOnClickListener { finish() }
        binding.importButton.setOnClickListener { openDocument.launch(arrayOf("application/json")) }
        binding.exportButton.setOnClickListener { createDocument.launch("wordgame-history.json") }
        binding.historyList.setOnItemClickListener { _, _, position, _ -> showDefinition(history[position].solution) }
        applyTheme()
        reload()
    }

    private fun showDefinition(word: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(word.uppercase())
            .setMessage("Loading definition…")
            .setPositiveButton("Close", null)
            .show()
        Thread {
            val definition = GameApi.fetchDefinition(word) ?: "No definition found."
            runOnUiThread { dialog.setMessage(definition) }
        }.start()
    }

    private fun isDark(): Boolean = when (repo.themeOverride()) {
        "light" -> false
        "dark" -> true
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyTheme() {
        val bgColor = ContextCompat.getColor(this, if (isDark()) R.color.bg_dark else R.color.bg_light)
        val textColor = if (isDark()) Color.WHITE else Color.BLACK
        val borderColor = ContextCompat.getColor(this, if (isDark()) R.color.border_dark else R.color.border_light)

        window.statusBarColor = bgColor
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = !isDark()
        binding.root.setBackgroundColor(bgColor)
        binding.topBar.setBackgroundColor(bgColor)
        binding.topDivider.setBackgroundColor(borderColor)
        binding.titleText.setTextColor(textColor)
        binding.emptyView.setTextColor(textColor)

        val iconTint = ColorStateList.valueOf(textColor)
        listOf(binding.backButton, binding.importButton, binding.exportButton).forEach { it.imageTintList = iconTint }
    }

    private fun reload() {
        history = repo.getHistory().sortedByDescending { it.date }
        binding.emptyView.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
        binding.historyList.adapter = HistoryAdapter(history)
    }

    private inner class HistoryAdapter(private val items: List<HistoryEntry>) : ArrayAdapter<HistoryEntry>(this@HistoryActivity, 0, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_history, parent, false)
            val entry = items[position]
            view.findViewById<ImageView>(R.id.resultIcon).setImageResource(if (entry.won) R.drawable.ic_check else R.drawable.ic_cancel)
            view.findViewById<TextView>(R.id.wordText).apply {
                text = entry.solution.uppercase()
                setTextColor(if (isDark()) Color.WHITE else Color.BLACK)
            }
            val guessWord = if (entry.guesses.size == 1) "guess" else "guesses"
            val hintNote = if (entry.hintUsed) " · hint used" else ""
            view.findViewById<TextView>(R.id.detailText).text = "${entry.date} — ${entry.guesses.size} $guessWord$hintNote"
            return view
        }
    }
}
