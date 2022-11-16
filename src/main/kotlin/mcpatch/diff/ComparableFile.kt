package mcpatch.diff

import java.io.InvalidObjectException

/**
 * 用来进行文件对比的抽象类
 */
abstract class ComparableFile
{
    /**
     * 文件名
     */
    abstract val name: String

    /**
     * 文件长度
     */
    abstract val length: Long

    /**
     * 文件哈希
     */
    abstract val hash: String

    /**
     * 文件修改时间
     */
    abstract val modified: Long

    /**
     * 子文件
     */
    abstract val files: List<ComparableFile>

    /**
     * 是否是一个文件
     */
    abstract val isFile: Boolean

    /**
     * 获取相对目录
     */
    abstract val relativePath: String

    /**
     * 删除一个文件
     * @param relativePath 相对路径
     */
    abstract fun removeFile(relativePath: String);

    /**
     * 获取子文件（仅目录支持这个方法）
     */
    abstract operator fun get(path: String): ComparableFile?


    /**
     * 获取相对路径下的文件
     * @param path 相对路径
     * @return 文件，如果找不到返回null
     * @throws InvalidObjectException 如果当前对象不是一个目录
     */
    protected fun getFileInternal(path: String): ComparableFile?
    {
        if (isFile)
            throw InvalidObjectException("the file named '$name' is not a directory, is a file.")

        val split = path.replace("\\", "/").split("/")
        var currentDir = this

        for ((index, name) in split.withIndex())
        {
            val reachEnd = index == split.size - 1
            val current = currentDir.files.firstOrNull { it.name == name } ?: return null
            if (!reachEnd) currentDir = current else return current
        }

        return null
    }
}