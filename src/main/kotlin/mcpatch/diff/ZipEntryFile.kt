package mcpatch.diff

import mcpatch.extension.StreamExtension.actuallySkip
import mcpatch.stream.CRC32CheckInputStream
import mcpatch.utils.HashUtils
import org.apache.commons.compress.archivers.zip.ZipFile

/**
 * 代表一个位于zip文件里的文件
 */
class ZipEntryFile(
    override val name: String,
    override val length: Long,
    override val hash: String,
    override val modified: Long,
    override val files: MutableList<ZipEntryFile>,
    override val isFile: Boolean,
    override val relativePath: String
) : ComparableFile() {

    fun hierarchy()
    {
        fun println(z: ZipEntryFile, indent: Int)
        {
            val sb = StringBuffer()
            for (i in 0 until indent)
                sb.append("    ")
            val i = sb.toString()

            println(i + "${z.name}  : ${z.relativePath}")
            if (!z.isFile)
                for (sub in z.files)
                    println(sub, indent + 1)
        }

        println(this, 0)
    }

    override fun toString(): String {
        return name + (if (!isFile) " (${files.size})" else "")
    }

    companion object {
        fun CreateFromZipFile(zip: ZipFile): ZipEntryFile
        {
            val root = ZipEntryFile("root", 0, "", 0, mutableListOf(), false, "")

            for (entry in zip.entries)
            {
                // 目录entry不做处理，因为后面会自动创建
                if (entry.name.endsWith("/"))
                    continue

                val splitName = entry.name.split("/")
                var parent: ZipEntryFile = root

                // 寻找父目录并创建父目录
                for (frag in splitName.dropLast(1))
                {
                    val find = parent.find(frag) as ZipEntryFile?

                    if (find != null)
                    {
                        parent = find
                        continue
                    }

                    val relativePath = parent.relativePath + (if (parent.relativePath.isNotEmpty()) "/" else "") + frag
                    val sub = ZipEntryFile(frag, 0, "", 0, mutableListOf(), false, relativePath)
                    parent.files.add(sub)
                    parent = sub
                }

                // 添加压缩包内文件数据
                val name = splitName.last()
                val file = ZipEntryFile(
                    name = name,
                    length = entry.size,
                    hash = HashUtils.crc32str(if (entry.crc != -1L) entry.crc else
                        CRC32CheckInputStream(zip.getInputStream(entry)).use { it.actuallySkip(entry.size); it.digest() } ),
                    modified = entry.lastModifiedTime.toMillis(),
                    files = mutableListOf(),
                    isFile = true,
                    relativePath = parent.relativePath + (if (parent.relativePath.isNotEmpty()) "/" else "") + name
                )

                parent.files.add(file)
            }

            return root
        }
    }
}
