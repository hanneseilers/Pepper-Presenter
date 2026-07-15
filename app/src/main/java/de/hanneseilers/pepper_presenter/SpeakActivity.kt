package de.hanneseilers.pepper_presenter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.`object`.touch.TouchSensor
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy

/**
 * Full-screen black activity that speaks a presentation sentence by sentence.
 *
 * Every sentence — including the first — is triggered by touching the head
 * sensor or one of the three foot bumpers.  After the last sentence has been
 * spoken, one additional touch returns to [MainActivity].
 */
class SpeakActivity : RobotActivity(), RobotLifecycleCallbacks {

    companion object {
        private const val EXTRA_SENTENCES = "sentences"

        /** Creates an Intent that launches [SpeakActivity] with the given sentences. */
        fun createIntent(context: Context, sentences: ArrayList<String>): Intent =
            Intent(context, SpeakActivity::class.java)
                .putStringArrayListExtra(EXTRA_SENTENCES, sentences)
    }

    // Names of the touch sensors we listen to
    private val sensorNames = listOf(
        "Head/Touch",
        "Bumper/FrontLeft",
        "Bumper/FrontRight",
        "Bumper/Back"
    )

    private lateinit var sentences: List<String>
    private var currentIndex = -1

    /** True while the robot is currently speaking a sentence. */
    @Volatile
    private var isSpeaking = false

    /**
     * True after a sentence has finished and the activity is waiting for
     * the user to trigger the next step via a touch/bumper event.
     */
    @Volatile
    private var isWaitingForTouch = false

    /**
     * True after the last sentence has been spoken.
     * The next touch/bumper event will finish this activity.
     */
    @Volatile
    private var isComplete = false

    /** Sensors registered in the current robot-focus session; cleared on focus loss. */
    private val registeredSensors = mutableListOf<TouchSensor>()

    // ── Activity lifecycle ───────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.IMMERSIVE)

        // Keep screen on and make it fully black (hide system UI)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_speak)

        val rawSentences = intent.getStringArrayListExtra(EXTRA_SENTENCES) ?: arrayListOf()
        sentences = rawSentences.filter { it.isNotBlank() }

        if (sentences.isEmpty()) {
            finish()
            return
        }

        QiSDK.register(this, this)
    }

    override fun onDestroy() {
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    // ── RobotLifecycleCallbacks ──────────────────────────────────────────────

    override fun onRobotFocusGained(robotContext: QiContext) {
        setupTouchListeners(robotContext)
        // Wait for the first sensor touch before speaking the first sentence
        isWaitingForTouch = true
    }

    override fun onRobotFocusLost() {
        registeredSensors.forEach { it.removeAllOnStateChangedListeners() }
        registeredSensors.clear()
    }

    override fun onRobotFocusRefused(reason: String?) = Unit

    // ── Touch / Bumper listeners ─────────────────────────────────────────────

    private fun setupTouchListeners(robotContext: QiContext) {
        val touch = robotContext.touch
        sensorNames.forEach { name ->
            try {
                val sensor = touch.getSensor(name)
                sensor.addOnStateChangedListener { state ->
                    if (state.touched) handleAdvance(robotContext)
                }
                registeredSensors.add(sensor)
            } catch (_: Exception) {
                // Sensor not present on this robot configuration — skip silently
            }
        }
    }

    // ── Presentation flow ────────────────────────────────────────────────────

    /**
     * Called on every touch/bumper "pressed" event.
     * Synchronized to prevent two rapid touches from both advancing the index.
     */
    @Synchronized
    private fun handleAdvance(robotContext: QiContext) {
        if (!isWaitingForTouch) return
        isWaitingForTouch = false

        if (isComplete) {
            runOnUiThread { finish() }
            return
        }

        currentIndex++
        if (currentIndex < sentences.size) {
            speakCurrent(robotContext)
        } else {
            // Defensive: all sentences already spoken — wait for one more touch to exit
            isComplete = true
            isWaitingForTouch = true
        }
    }

    /** Speaks [sentences][currentIndex] and sets state flags before and after. */
    private fun speakCurrent(robotContext: QiContext) {
        isSpeaking = true
        isWaitingForTouch = false
        SayBuilder.with(robotContext)
            .withPhrase(Phrase(VoiceSettings.applyToText(this, sentences[currentIndex])))
            .buildAsync()
            .andThenCompose { say: Say -> say.async().run() }
            .thenConsume {
                isSpeaking = false
                isComplete = currentIndex >= sentences.size - 1
                isWaitingForTouch = true
            }
    }

    // ── System UI helpers ────────────────────────────────────────────────────

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }
}
