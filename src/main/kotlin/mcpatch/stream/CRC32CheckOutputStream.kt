package mcpatch.stream

import mcpatch.utils.HashUtils
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32

class CRC32CheckOutputStream(val output: OutputStream) : OutputStream()
{
    val crc32 = CRC32()

    fun digest(): Long
    {
        return crc32.value
    }

    override fun write(b: Int)
    {
        crc32.update(b)
        output.write(b)
    }
}