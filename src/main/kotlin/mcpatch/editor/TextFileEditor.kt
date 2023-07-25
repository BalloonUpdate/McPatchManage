package mcpatch.editor

import java.io.File

class TextFileEditor(val file: File)
{
    /**
     * 获取文件内容
     */
    fun get(): String?
    {
        if (!file.exists())
            return null

        val content = file.readText()

        if (content.isEmpty())
            return null

        return content.replace("\r\n", "\n").replace("\r", "\n")
    }

    fun clear()
    {
        file.delete()
        file.createNewFile()
    }

    fun create()
    {
        file.createNewFile()
    }

    fun write(text: String)
    {
        file.writeText(text)
    }

    fun delete()
    {
        file.delete()
    }
}