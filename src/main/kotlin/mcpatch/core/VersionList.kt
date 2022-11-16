package mcpatch.core

import mcpatch.utils.File2

class VersionList(val versionsFile: File2)
{
    /**
     * 所有的版本号列表
     */
    val versions = mutableListOf<String>()

    /**
     * 获取最新的三个版本号
     */
    fun getNewest3(): List<String>
    {
        return if (versions.isNotEmpty()) versions.takeLast(3).reversed() else listOf()
    }

    /**
     * 从文件重新加载版本号列表
     */
    fun reload()
    {
        versions.clear()
        versions.addAll(if (versionsFile.exists)
            versionsFile.content.trim().split("\n").filter { it.isNotEmpty() }
        else
            listOf())
    }

    /**
     * 保存对版本的修改
     */
    fun save()
    {
        versionsFile.content = versions.joinToString("\n")
    }

    /**
     * 检查是否包含某个版本号
     */
    operator fun contains(version: String): Boolean = version in versions
}