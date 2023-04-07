package mcpatch.editor

import mcpatch.utils.File2

class TextFileEditor(val file: File2) : AutoCloseable
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

    fun clear()
    {
        file.delete()
        file.create()
    }

    fun open()
    {
        file.create()
    }

    fun write(text: String)
    {
        file.content = text
    }

    override fun close()
    {
        file.delete()
    }
}