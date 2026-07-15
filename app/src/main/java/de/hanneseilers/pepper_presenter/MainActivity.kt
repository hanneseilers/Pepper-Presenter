package de.hanneseilers.pepper_presenter

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.aldebaran.qi.sdk.`object`.conversation.Say
import com.aldebaran.qi.sdk.design.activity.RobotActivity
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.design.activity.conversationstatus.SpeechBarDisplayStrategy

class MainActivity : RobotActivity(), RobotLifecycleCallbacks,
    PresentationWebSocketServer.Listener {

    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var bottomSpacer: View
    private lateinit var buttonNew: Button
    private lateinit var buttonLoad: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonDelete: Button
    private lateinit var buttonSpeak: Button
    private lateinit var buttonSettings: ImageButton
    private lateinit var buttonWsSend: ImageButton
    private lateinit var buttonWsCancel: ImageButton

    private val presentationManager = PresentationManager()

    @Volatile
    private var robotContext: QiContext? = null

    private var wsServer: PresentationWebSocketServer? = null

    companion object {
        private const val WS_PORT = 8887
        private const val BOTTOM_SPACER_HEIGHT_DP = 72
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSpeechBarDisplayStrategy(SpeechBarDisplayStrategy.IMMERSIVE)
        setContentView(R.layout.activity_main)
        QiSDK.register(this, this)

        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        bottomSpacer = findViewById(R.id.bottomSpacer)
        buttonNew = findViewById(R.id.buttonNew)
        buttonLoad = findViewById(R.id.buttonLoad)
        buttonSave = findViewById(R.id.buttonSave)
        buttonDelete = findViewById(R.id.buttonDelete)
        buttonSpeak = findViewById(R.id.buttonSpeak)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonWsSend = findViewById(R.id.buttonWsSend)
        buttonWsCancel = findViewById(R.id.buttonWsCancel)

        buttonNew.setOnClickListener { clearPresentation() }
        buttonLoad.setOnClickListener { showLoadDialog() }
        buttonSave.setOnClickListener { savePresentation() }
        buttonDelete.setOnClickListener { showDeleteDialog() }
        buttonSpeak.setOnClickListener { speakPresentation() }
        buttonSettings.setOnClickListener { showVoiceSettingsDialog() }
        buttonWsSend.setOnClickListener { sendViaWebSocket() }
        buttonWsCancel.setOnClickListener { closeWebSocketConnection() }

        applySystemInsets()
        startWebSocketServer()
    }

    override fun onDestroy() {
        stopWebSocketServer()
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    // ── WebSocket server ─────────────────────────────────────────────────────

    private fun startWebSocketServer() {
        val server = PresentationWebSocketServer(WS_PORT, this)
        server.isReuseAddr = true
        server.start()
        wsServer = server
    }

    private fun stopWebSocketServer() {
        try {
            wsServer?.stop()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        wsServer = null
    }

    override fun onClientConnected() {
        runOnUiThread {
            buttonWsSend.visibility = View.VISIBLE
            buttonWsCancel.visibility = View.VISIBLE
        }
    }

    override fun onClientDisconnected() {
        runOnUiThread {
            buttonWsSend.visibility = View.INVISIBLE
            buttonWsCancel.visibility = View.INVISIBLE
        }
    }

    override fun onMessageReceived(title: String, text: String) {
        runOnUiThread {
            editTextTitle.setText(title)
            editTextContent.setText(text)
        }
    }

    private fun sendViaWebSocket() {
        val server = wsServer ?: return
        val title = editTextTitle.text.toString()
        val text = editTextContent.text.toString()
        val success = server.sendPresentation(title, text)
        val msgRes = if (success) R.string.msg_ws_sent else R.string.msg_ws_send_error
        Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show()
    }

    private fun closeWebSocketConnection() {
        wsServer?.closeActiveConnection()
    }

    private fun applySystemInsets() {
        val density = resources.displayMetrics.density
        val baseHeight = (BOTTOM_SPACER_HEIGHT_DP * density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(bottomSpacer) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.layoutParams = view.layoutParams.apply {
                height = baseHeight + bottomInset
            }
            insets
        }
        ViewCompat.requestApplyInsets(bottomSpacer)
    }

    private fun showVoiceSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_voice_settings, null)
        val speedLabel = dialogView.findViewById<TextView>(R.id.textVoiceSpeed)
        val pitchLabel = dialogView.findViewById<TextView>(R.id.textVoicePitch)
        val speedSeek = dialogView.findViewById<SeekBar>(R.id.seekVoiceSpeed)
        val pitchSeek = dialogView.findViewById<SeekBar>(R.id.seekVoicePitch)

        val currentSpeed = VoiceSettings.getSpeed(this)
        val currentPitch = VoiceSettings.getPitch(this)
        speedSeek.progress = currentSpeed - VoiceSettings.MIN_VALUE
        pitchSeek.progress = currentPitch - VoiceSettings.MIN_VALUE

        fun updateLabels() {
            val speed = VoiceSettings.MIN_VALUE + speedSeek.progress
            val pitch = VoiceSettings.MIN_VALUE + pitchSeek.progress
            speedLabel.text = getString(R.string.voice_speed_format, speed)
            pitchLabel.text = getString(R.string.voice_pitch_format, pitch)
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }

        speedSeek.setOnSeekBarChangeListener(listener)
        pitchSeek.setOnSeekBarChangeListener(listener)
        updateLabels()

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_voice_settings_title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                VoiceSettings.save(
                    this,
                    VoiceSettings.MIN_VALUE + speedSeek.progress,
                    VoiceSettings.MIN_VALUE + pitchSeek.progress
                )
            }
            .show()
    }

    // ── RobotLifecycleCallbacks ──────────────────────────────────────────────

    override fun onRobotFocusGained(context: QiContext) {
        robotContext = context
        SayBuilder.with(context)
            .withPhrase(Phrase(VoiceSettings.applyToText(this, "Es kann losgehen")))
            .buildAsync()
            .andThenCompose {
                    say: Say -> say.async().run()
            }
            .thenConsume { future ->
                if (future.hasError()) {
                    future.error.printStackTrace()
                }
            }
    }

    override fun onRobotFocusLost() {
        robotContext = null
    }

    override fun onRobotFocusRefused(reason: String?) = Unit

    // ── UI actions ───────────────────────────────────────────────────────────

    private fun clearPresentation() {
        editTextTitle.setText("")
        editTextContent.setText("")
        editTextTitle.requestFocus()
    }

    private fun savePresentation() {
        val title = editTextTitle.text.toString().trim()
        val content = editTextContent.text.toString()

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.msg_title_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.msg_content_required, Toast.LENGTH_SHORT).show()
            return
        }

        val success = presentationManager.save(this, Presentation(title, content))
        val msgRes = if (success) R.string.msg_saved else R.string.msg_save_error
        Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show()
    }

    private fun showLoadDialog() {
        val fileNames = presentationManager.listFileNames(this)
        if (fileNames.isEmpty()) {
            Toast.makeText(this, R.string.dialog_no_presentations, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_load_title)
            .setItems(fileNames.toTypedArray()) { _, index ->
                Thread {
                    val presentation = presentationManager.load(this, fileNames[index])
                    runOnUiThread {
                        if (presentation != null) {
                            editTextTitle.setText(presentation.title)
                            editTextContent.setText(presentation.content)
                        } else {
                            Toast.makeText(this, R.string.msg_load_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
            .show()
    }

    private fun showDeleteDialog() {
        val fileNames = presentationManager.listFileNames(this)
        if (fileNames.isEmpty()) {
            Toast.makeText(this, R.string.dialog_no_presentations, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setItems(fileNames.toTypedArray()) { _, index ->
                val success = presentationManager.delete(this, fileNames[index])
                val msgRes = if (success) R.string.msg_deleted else R.string.msg_delete_error
                Toast.makeText(this, msgRes, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun speakPresentation() {
        if (robotContext == null) {
            Toast.makeText(this, R.string.msg_robot_not_ready, Toast.LENGTH_SHORT).show()
            return
        }

        val content = editTextContent.text.toString()
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.msg_content_required, Toast.LENGTH_SHORT).show()
            return
        }

        val sentences = ArrayList(content.split("\n").map { it.trim() }.filter { it.isNotBlank() })
        closeWebSocketConnection()
        startActivity(SpeakActivity.createIntent(this, sentences))
    }
}
