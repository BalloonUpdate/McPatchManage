package mcpatch.core

import mcpatch.utils.File2

class VersionList(val file: File2)
{
    fun read(): MutableList<String>
    {
        if (!file.exists)
            return mutableListOf()

        return file.content
            .trim()
            .split("\r\n", "\n", "\r")
            .filter { it.isNotEmpty() }
            .map { it.trim() }
            .toMutableList()
    }

    fun write(versions: List<String>)
    {
        file.content = versions.joinToString("\n")
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