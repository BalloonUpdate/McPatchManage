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

    override fun removeFile(relativePath: String) = (file + relativePath).delete()

    override fun get(path: String): RealFile? = getFileInternal(path) as RealFile?

    /**
     * 应用一个Diff对象
     * @param diff Diff对象
     * @param source 参照目录
     */
    fun applyDiff(diff: DirectoryDiff, source: File2)
    {
        for (f in diff.oldFiles)
            (file + f).delete()

        for (f in diff.oldFolders)
            (file + f).delete()

        for (f in diff.newFolders)
            (file + f).mkdirs()

        for (f in diff.newFiles)
            (source + f).copy(file + f)
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