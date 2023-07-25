package mcpatch.diff

import mcpatch.classextension.FileExtension.moveToOverwrite
import mcpatch.utility.HashUtils
import java.io.File

/**
 * 真实文件对象
 */
class RealFile(val file: File, parent: RealFile?, childrenLinkThis: Boolean) : ComparableFile()
{
    override val name: String by lazy { file.name }
    override val length: Long by lazy { file.length() }
    override val hash: String by lazy { HashUtils.crc32(file) }
    override val modified: Long by lazy { file.lastModified() }
    override val files: List<RealFile> by lazy { file.listFiles()!!.map { RealFile(it, if (childrenLinkThis) this else null, true) } }
    override val isFile: Boolean by lazy { file.isFile }
    override val relativePath: String = (if (parent != null) parent.relativePath + "/" else "") + name

    /**
     * 将本目录作为variable目录，应用一个Diff对象
     */
    fun syncFrom(diff: DirectoryDiff, invariable: File)
    {
        for ((from, to) in diff.moveFiles)
            File(file, from).moveToOverwrite(File(file, to))

        for (f in diff.redundantFiles)
            File(file, f).delete()

        for (f in diff.redundantFolders)
            File(file, f).delete()

        for (f in diff.missingFolders)
            File(file, f).mkdirs()

        for (f in diff.missingFiles)
            File(invariable, f).copyTo(File(file, f))

    }

    companion object {
        /**
         * 从磁盘文件对象创建
         */
        @JvmStatic
        fun CreateFromRealFile(file: File): RealFile
        {
            return RealFile(file, null, false)
        }
    }
}