package mcpatch.editor

import mcpatch.utils.File2

class ExternalTextFileEditor(val file: File2) : AutoCloseable
{
    /**
     * 获取文件内容
     */
    fun get(): String?
    {
        if (!file.exists)
            return null

        val content = file.content

        if (content.isEmpty())
            return null

        return content.replace("\r\n", "\n").replace("\r", "\n")
    }

    fun open()
    {
        file.create()
    }

    override fun close()
    {
        file.delete()
    }
}