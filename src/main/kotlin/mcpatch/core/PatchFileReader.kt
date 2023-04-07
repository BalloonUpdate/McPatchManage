package mcpatch.core

import mcpatch.data.ModificationMode
import mcpatch.data.NewFile
import mcpatch.data.VersionData
import mcpatch.exception.McPatchManagerException
import mcpatch.extension.StreamExtension.copyAmountTo
import mcpatch.utils.File2
import mcpatch.utils.HashUtils
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.tools.bzip2.CBZip2InputStream
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class PatchFileReader(val version: String, val file: File2) : Iterable<PatchFileReader.PatchEntry>, AutoCloseable
{
    val archive = ZipFile(file.file, "utf-8")
    val meta: VersionData

    init {
        // 读取元数据
        val metaEntry = archive.getEntry(".mcpatch-meta.json") ?: throw McPatchManagerException("找不到更新包的元数据")
        val metaSize = metaEntry.size.toInt()
        val metaBuf = ByteArrayOutputStream(metaSize)
        archive.getInputStream(metaEntry).use { it.copyAmountTo(metaBuf, 4 * 1024, metaSize.toLong()) }
        meta = VersionData(JSONObject(metaBuf.toByteArray().decodeToString()))
    }

    override fun iterator(): Iterator<PatchEntry>
    {
        return NewFileIterator(this)
    }

    override fun close()
    {
        archive.close()
    }

    class NewFileIterator(val reader: PatchFileReader) : Iterator<PatchEntry>
    {
        val iter = reader.meta.newFiles.iterator()

        override fun hasNext(): Boolean = iter.hasNext()

        override fun next(): PatchEntry = PatchEntry(reader, iter.next())
    }

    class PatchEntry(private val reader: PatchFileReader, val newFile: NewFile)
    {
        fun read(output: OutputStream)
        {
            val entry = reader.archive.getEntry(newFile.path) ?: throw McPatchManagerException("[${reader.version}] 找不到文件数据: $newFile")

            reader.archive.getInputStream(entry).use { stream ->
                if (newFile.mode != ModificationMode.Modify && newFile.mode != ModificationMode.Fill)
                    return

                ByteArrayOutputStream().use { temp ->
                    // 提取bzipped数据
                    temp.reset()
                    stream.copyAmountTo(temp, 128 * 1024, entry.size)

                    // 检查bzipped数据
                    val bzipped = temp.toByteArray()
                    if (HashUtils.sha1(bzipped) != newFile.bzippedHash)
                        throw McPatchManagerException("[${reader.version}] 更新包中 ${newFile.path} 文件的数据（bzipped）无法通过验证")

                    // 检查raw数据
                    ByteArrayInputStream(bzipped).use { bzippedInput ->
                        ByteArrayOutputStream().use { raw ->
                            val bzip = CBZip2InputStream(bzippedInput)
                            bzip.copyAmountTo(raw, 128 * 1024, newFile.rawLength)

                            if (HashUtils.sha1(raw.toByteArray()) != newFile.rawHash)
                                throw McPatchManagerException("[${reader.version}] 更新包中 ${newFile.path} 文件的数据（解压缩后）无法通过验证")

                            output.write(raw.toByteArray())
                        }
                    }
                }
            }
        }
    }
}