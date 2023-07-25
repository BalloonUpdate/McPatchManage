package mcpatch.core

import java.io.File

class VersionList(val file: File)
{
    fun read(): MutableList<String>
    {
        if (!file.exists())
            return mutableListOf()

        return file.readText()
            .trim()
            .split("\r\n", "\n", "\r")
            .filter { it.isNotEmpty() }
            .map { it.trim() }
            .toMutableList()
    }

    fun write(versions: List<String>)
    {
        file.writeText(versions.joinToString("\n"))
    }

    /**
     * 获取最新的三个版本号
     */
    fun getNewest(): String?
    {
        val versions = read()
        return if (versions.isNotEmpty()) versions.last() else null
    }
}