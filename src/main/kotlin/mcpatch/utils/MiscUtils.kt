package mcpatch.utils

object MiscUtils
{
    /**
     * 拆分较长的字符串到多行里
     */
    @JvmStatic
    fun stringBreak(str: String, lineLength: Int, newline: String="\n"): String
    {
        val lines = mutableListOf<String>()

        val lineCount = str.length / lineLength
        val remains = str.length % lineLength

        for (i in 0 until lineCount)
            lines += str.substring(lineLength * i, lineLength * (i + 1))

        if (remains > 0)
            lines += str.substring(lineLength * lineCount)

        return lines.joinToString(newline)
    }

    /**
     * 字节转换为kb, mb, gb等单位
     */
    @JvmOverloads
    fun convertBytes(bytes: Long, b: String = "B", kb: String = "KB", mb: String = "MB", gb: String = "GB"): String
    {
        return when {
            bytes < 1024 -> "$bytes $b"
            bytes < 1024 * 1024 -> "${String.format("%.2f", (bytes / 1024f))} $kb"
            bytes < 1024 * 1024 * 1024 -> "${String.format("%.2f", (bytes / 1024 / 1024f))} $mb"
            else -> "${String.format("%.2f", (bytes / 1024 / 1024 / 1024f))} $gb"
        }
    }
}