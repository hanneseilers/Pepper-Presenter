package de.hanneseilers.pepper_presenter

import android.content.Context

object VoiceSettings {
    private const val PREFS_NAME = "voice_settings"
    private const val KEY_SPEED = "speed"
    private const val KEY_PITCH = "pitch"
    const val MIN_VALUE = 50
    const val MAX_VALUE = 200
    const val DEFAULT_VALUE = 100

    fun getSpeed(context: Context): Int =
        context.preferences().getInt(KEY_SPEED, DEFAULT_VALUE).coerceIn(MIN_VALUE, MAX_VALUE)

    fun getPitch(context: Context): Int =
        context.preferences().getInt(KEY_PITCH, DEFAULT_VALUE).coerceIn(MIN_VALUE, MAX_VALUE)

    fun save(context: Context, speed: Int, pitch: Int) {
        context.preferences()
            .edit()
            .putInt(KEY_SPEED, speed.coerceIn(MIN_VALUE, MAX_VALUE))
            .putInt(KEY_PITCH, pitch.coerceIn(MIN_VALUE, MAX_VALUE))
            .apply()
    }

    fun applyToText(context: Context, text: String): String {
        val speed = getSpeed(context)
        val pitch = getPitch(context)
        return "\\rspd=$speed\\ \\vct=$pitch\\ $text"
    }

    private fun Context.preferences() =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
