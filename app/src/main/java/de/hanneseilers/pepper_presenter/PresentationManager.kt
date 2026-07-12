package de.hanneseilers.pepper_presenter

import android.content.Context
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.conversation.Say
import java.io.File
import java.io.IOException

/**
 * Manages saving, loading and speaking of [Presentation] objects.
 *
 * Presentations are stored in the app's private files directory as plain text files.
 * The first line of each file is the title; the remaining lines are the content.
 * File names are derived from the title by replacing non-alphanumeric characters
 * with underscores and appending ".txt".
 */
class PresentationManager {

    // ── Storage ─────────────────────────────────────────────────────────────

    /** Returns the directory used to store presentations, creating it if necessary. */
    private fun storageDir(context: Context): File =
        File(context.filesDir, "presentations").also { it.mkdirs() }

    /** Converts a title to a safe file name (e.g. "My Talk!" → "My_Talk_.txt"). */
    private fun titleToFileName(title: String): String =
        title.trim().replace(Regex("[^A-Za-z0-9._\\- ]"), "_") + ".txt"

    /**
     * Saves [presentation] to internal storage.
     * @return `true` on success, `false` on error.
     */
    fun save(context: Context, presentation: Presentation): Boolean {
        return try {
            val file = File(storageDir(context), titleToFileName(presentation.title))
            file.writeText("${presentation.title}\n${presentation.content}")
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Loads a presentation from [file].
     * @return The loaded [Presentation], or `null` if the file cannot be read.
     */
    fun load(context: Context, fileName: String): Presentation? {
        return try {
            val file = File(storageDir(context), fileName)
            val lines = file.readLines()
            if (lines.isEmpty()) return null
            val title = lines.first()
            val content = lines.drop(1).joinToString("\n")
            Presentation(title, content)
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Returns the file names of all saved presentations, sorted alphabetically.
     */
    fun listFileNames(context: Context): List<String> =
        storageDir(context).list()
            ?.filter { it.endsWith(".txt") }
            ?.sorted()
            ?: emptyList()

    // ── Speech ───────────────────────────────────────────────────────────────

    /**
     * Makes Pepper say [text] using the provided [robotContext].
     * The call is asynchronous and returns immediately.
     * Errors during speech synthesis are silently ignored.
     */
    fun speak(robotContext: QiContext, text: String) {
        SayBuilder.with(robotContext)
            .withPhrase(Phrase(text))
            .buildAsync()
            .andThenCompose { say: Say -> say.async().run() }
            .thenConsume { /* errors are intentionally swallowed here */ }
    }
}
