package mcpatch.core

import mcpatch.utils.File2

class OverwriteFile(val file: File2)
{
    fun read(): List<String>
    {
        if (!file.exists)
            return listOf()

        return file.file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun empty()
    {
        file.delete()
    }
}