package mcpatch

import fi.iki.elonen.NanoWSD
import mcpatch.utility.CapturedLogging

class InteractiveExecutor(val websocket: WebsocketServe) {
    private val log = CapturedLogging()
    private var task: Task? = null
    private val runnable = Runnable {
        try {
            task!!.log = log
            task!!.run()
        } catch (e: Exception) {
            websocket
            onPrint(e.toString())
        } finally {
            task = null
            websocket.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "finish", true)
        }
    }
    private val thread = Thread(runnable, "longtimetask")

    fun run(task: Task) {
        if (this.task != null)
            throw RuntimeException("another task is been executing now")

        this.task = task
        thread.start()
    }

    fun getRunningTask(): Task? {
        return task
    }

    fun feedInput(input: String) {
        task?.onInput(input)
    }

    fun isRunning(): Boolean {
        return task != null
    }

    fun terminal() {

    }

    abstract class Task : Runnable {
        lateinit var log: CapturedLogging
        val inputLines = mutableListOf<String>()

        fun onInput(content: String) {
            inputLines.add(content)
        }

        fun readAnyLine(): String {

        }
    }
}