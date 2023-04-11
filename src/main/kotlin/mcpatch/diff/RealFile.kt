package mcpatch.diff

import mcpatch.utils.File2
import mcpatch.utils.HashUtils

/**
 * 真实文件对象
 */
class RealFile(val file: File2, parent: RealFile?, childrenLinkThis: Boolean) : ComparableFile()
{
    override val name: String by lazy { file.name }
    override val length: Long by lazy { file.length }
    override val hash: String by lazy { HashUtils.crc32(file.file) }
    override val modified: Long by lazy { file.modified }
    override val files: List<RealFile> by lazy { file.files.map { RealFile(it, if (childrenLinkThis) this else null, true) } }
    override val isFile: Boolean by lazy { file.isFile }
    override val relativePath: String = (if (parent != null) parent.relativePath + "/" else "") + name

    /**
     * 将本目录作为variable目录，应用一个Diff对象
     */
    fun syncFrom(diff: DirectoryDiff, invariable: File2)
    {
        for ((from, to) in diff.moveFiles)
            (file + from).move(file + to)

        for (f in diff.redundantFiles)
            (file + f).delete()

        for (f in diff.redundantFolders)
            (file + f).delete()

        for (f in diff.missingFolders)
            (file + f).mkdirs()

        for (f in diff.missingFiles)
            (invariable + f).copy(file + f)

    }

    companion object {
        /**
         * 从磁盘文件对象创建
         */
        @JvmStatic
        fun CreateFromRealFile(file: File2): RealFile
        {
            return RealFile(file, null, false)
        }
    }
}