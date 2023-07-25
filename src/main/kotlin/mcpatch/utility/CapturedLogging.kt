package mcpatch.utility

import java.util.*

class CapturedLogging {
    val rangedTags = LinkedList<String>()
    val captured = mutableListOf<String>()

    @JvmOverloads
    fun debug(message: Any, newLine: Boolean = true) = message(LogLevel.DEBUG, "", message.toString(), newLine)

    @JvmOverloads
    fun info(message: Any, newLine: Boolean = true) = message(LogLevel.INFO, "", message.toString(), newLine)

    @JvmOverloads
    fun warn(message: Any, newLine: Boolean = true) = message(LogLevel.WARN, "", message.toString(), newLine)

    @JvmOverloads
    fun error(message: Any, newLine: Boolean = true) = message(LogLevel.ERROR, "", message.toString(), newLine)

    fun message(level: LogLevel, tag: String, content: String, newLine: Boolean) {
        if (!newLine) {
            captured.add(captured.removeLast() + content)
            return
        }

        val tagText = if (tag.isNotEmpty()) "[${tag}] " else ""
        val rangedTags = rangedTags.joinToString("/").run { if (isNotEmpty()) "[$this] " else "" }
        val prefix = String.format("%s%s", rangedTags, tagText)
        val text = (prefix + content).replace("\n", "\n"+prefix)

        captured.add(text)
    }

    fun openTag(tag: String) {
        if (rangedTags.lastOrNull().run { this == null || this != tag })
            rangedTags.addLast(tag)
    }

    fun closeTag() {
        if (rangedTags.isNotEmpty())
            rangedTags.removeLast()
    }

    enum class LogLevel
    {
        ALL, DEBUG, INFO, WARN, ERROR, NONE
    }
}