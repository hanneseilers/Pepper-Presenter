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
        activeConnection?.close()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        ex.printStackTrace()
    }

    override fun onStart() {
        // server started successfully
    }

    fun sendPresentation(title: String, text: String): Boolean {
        val conn = activeConnection ?: return false
        return try {
            val json = org.json.JSONObject()
            json.put("title", title)
            json.put("text", text)
            conn.send(json.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun closeActiveConnection() {
        activeConnection?.close()
        activeConnection = null
    }
}
