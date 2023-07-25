package mcpatch.classextension

import java.io.*

object FileExtension
{
    @Suppress("NOTHING_TO_INLINE")
    inline fun File.bufferedInputStream(bufferSize: Int = 128 * 1024): BufferedInputStream
    {
        return FileInputStream(this).buffered(bufferSize)
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun File.bufferedOutputStream(bufferSize: Int = 128 * 1024): BufferedOutputStream
    {
        return FileOutputStream(this).buffered(bufferSize)
    }

    fun File.moveToOverwrite(target: File)
    {
        if(!exists())
            throw FileNotFoundException(path)

        if (target.exists())
            target.delete()

        target.parentFile.mkdirs()

        if (!renameTo(target))
            throw RuntimeException("fail to move file from $path to ${target.path}")
    }

}