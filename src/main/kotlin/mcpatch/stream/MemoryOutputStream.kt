package mcpatch.stream

import mcpatch.utils.HashUtils
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32

class MemoryOutputStream(initialSize: Int = 4 * 1024) : ByteArrayOutputStream()
{
    fun buffer(): ByteArray = buf

    fun sha1(): String
    {
        val sha1 = MessageDigest.getInstance("sha1")
        sha1.update(buf, 0, size())
        return HashUtils.bin2str(sha1.digest())
    }

    fun crc32(): Long
    {
        val crc32 = CRC32()
        crc32.update(buf, 0, size())
        return crc32.value
    }
}