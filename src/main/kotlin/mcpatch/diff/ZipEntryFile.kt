package mcpatch.diff

import mcpatch.utils.HashUtils
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry

class ZipEntryFile(entry: ZipArchiveEntry) : ComparableFile()
{
    override val name: String by lazy { entry.name }
    override val length: Long by lazy { entry.size }
    override val hash: String by lazy { crc32str(entry.crc) }
    override val modified: Long by lazy { entry.lastModifiedTime.toMillis() }
    override val files: List<ComparableFile> by lazy {  }
    override val isFile: Boolean by lazy {  }
    override val relativePath: String by lazy {  }

    override fun removeFile(relativePath: String) {
        TODO("Not yet implemented")
    }

    override fun get(path: String): ComparableFile? {
        TODO("Not yet implemented")
    }

    fun crc32str(value: Long): String
    {
        val array = ByteArray(4)
        array[3] = (value shr (8 * 0) and 0xFF).toByte()
        array[2] = (value shr (8 * 1) and 0xFF).toByte()
        array[1] = (value shr (8 * 2) and 0xFF).toByte()
        array[0] = (value shr (8 * 3) and 0xFF).toByte()

        return HashUtils.bin2str(array)
    }
}
