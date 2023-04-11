package mcpatch.extension

import org.apache.tools.bzip2.CBZip2OutputStream
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream

object StreamExtension
{
    fun InputStream.actuallySkip(n: Long)
    {
        var remains = n
        while (remains > 0)
            remains -= skip(remains)
    }

    fun InputStream.actuallyRead(buf: ByteArray, offset: Int, amount: Int): Int
    {
        var remains = amount

        while (remains > 0)
            remains -= read(buf, offset + (amount - remains), remains)

        return amount
    }

    fun InputStream.copyAmountTo(
        out: OutputStream,
        amount: Long,
        buffer: Int = 128 * 1024,
        callback: ((copied: Long, total: Long) -> Unit)? = null
    ): Long {
        if (amount == 0L)
            return 0L

        var bytesCopied: Long = 0
        val buf = ByteArray(buffer)

        val times = amount / buffer
        val remain = amount % buffer

        for (i in 0 until times)
        {
            val bytes = actuallyRead(buf, 0, buf.size)
            out.write(buf, 0, bytes)
            bytesCopied += bytes
            callback?.invoke(bytesCopied, amount)
        }

        if (remain > 0)
        {
            val bytes = actuallyRead(buf, 0, remain.toInt())
            out.write(buf, 0, bytes)
            bytesCopied += bytes
            callback?.invoke(bytesCopied, amount)
        }

        return bytesCopied
    }

//    fun CBZip2OutputStream.sha1(): String
//    {
//
//    }
}