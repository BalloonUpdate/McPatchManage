package mcpatch.stream

import java.io.InputStream
import java.util.zip.CRC32

class CRC32CheckInputStream (val input: InputStream) : InputStream()
{
    val crc32 = CRC32()

    fun digest(): Long
    {
        return crc32.value
    }

    override fun read(): Int
    {
        val value = input.read()

        if (value != -1)
            crc32.update(value)

        return value
    }

    override fun available(): Int
    {
        return input.available()
    }

    override fun mark(readlimit: Int)
    {
        input.mark(readlimit)
    }

    override fun markSupported(): Boolean
    {
        return input.markSupported()
    }

    override fun reset()
    {
        input.reset()
    }

    override fun close()
    {
        input.close()
    }
}