package de.hanneseilers.pepper_presenter

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class PresentationWebSocketServer(
    port: Int,
    private val listener: Listener
) : WebSocketServer(InetSocketAddress(port)) {

    interface Listener {
        fun onClientConnected()
        fun onClientDisconnected()
        fun onMessageReceived(title: String, text: String)
    }

    @Volatile
    private var activeConnection: WebSocket? = null

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        activeConnection = conn
        listener.onClientConnected()
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        if (activeConnection == conn) {
            activeConnection = null
        }
        listener.onClientDisconnected()
    }

    override fun onMessage(conn: WebSocket, message: String) {
        try {
            val json = org.json.JSONObject(message)
            val title = json.optString("title", "")
            val text = json.optString("text", "")
            listener.onMessageReceived(title, text)
        } catch (_: Exception) {
            // ignore malformed messages
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        ex.printStackTrace()
    }

    override fun onStart() {
        // server started successfully
    }

    fun sendPresentation(title: String, text: String) {
        val conn = activeConnection ?: return
        try {
            val json = org.json.JSONObject()
            json.put("title", title)
            json.put("text", text)
            conn.send(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeActiveConnection() {
        activeConnection?.close()
        activeConnection = null
    }
}
