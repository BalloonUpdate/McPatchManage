package mcpatch

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.json.JSONObject
import java.io.IOException

class WebsocketServe(handshake: NanoHTTPD.IHTTPSession) : NanoWSD.WebSocket(handshake) {
    val executor = InteractiveExecutor(this)

    override fun onOpen() {

    }

    override fun onClose(code: NanoWSD.WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean) {
        if (executor.isRunning())
            executor.terminal()
    }

    override fun onMessage(message: NanoWSD.WebSocketFrame) {
        if (message.opCode != NanoWSD.WebSocketFrame.OpCode.Text)
            return

        val payload = message.textPayload

        if (!executor.isRunning())
            return

        executor.feedInput(payload)
    }

    override fun onPong(pong: NanoWSD.WebSocketFrame) {

    }

    override fun onException(exception: IOException) {
        TODO("Not yet implemented")
    }

    fun buildRespond(success: Boolean, description: String): String {
        val response = JSONObject()

        response.put("status", success)
        response.put("description", description)

        return response.toString(4)
    }
}