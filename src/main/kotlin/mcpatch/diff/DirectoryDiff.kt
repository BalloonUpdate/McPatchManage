package mcpatch.diff

import mcpatch.core.IgnoreFile
import mcpatch.core.OverwriteFile
import mcpatch.utils.PathUtils

/**
 * 目录差异对比
 */
class DirectoryDiff
{
    val missingFolders: MutableList<String> = mutableListOf()
    val missingFiles: MutableList<String> = mutableListOf()
    val redundantFolders: MutableList<String> = mutableListOf()
    val redundantFiles: MutableList<String> = mutableListOf()
    val moveFiles: MutableList<Pair<String, String>> = mutableListOf()
    var totalDiff: Int = 0

    /**
     * 计算variable目录要变成到invariable目录之间的所有文件改动
     * @param variable 改动目录
     * @param invariable 参照目录
     * @param ignoresfile 要被忽略的文件列表
     * @param fileMovingSupport 启用文件移动支持
     * @return 有无差异
     */
    fun compare(variable: List<ComparableFile>, invariable: List<ComparableFile>, ignoresfile: IgnoreFile, fileMovingSupport: Boolean): Boolean
    {
        findMissings(invariable, variable)
        findRedundants(invariable, variable)

        // 处理文件忽略
        ignoresfile.reload()
        missingFolders.removeIf { it in ignoresfile }
        missingFiles.removeIf { it in ignoresfile }
        redundantFolders.removeIf { it in ignoresfile }
        redundantFiles.removeIf { it in ignoresfile }

        // 处理文件移动
        if (fileMovingSupport)
            detectFileMovings(invariable, variable)

        totalDiff = missingFolders.size + missingFiles.size + redundantFolders.size + redundantFiles.size + moveFiles.size
        return totalDiff > 0
    }

    /**
     * 寻找缺少的文件
     */
    private fun findMissings(invariable: List<ComparableFile>, variable: List<ComparableFile>)
    {
        for (i in invariable)
        {
            val v = variable.firstOrNull { it.name == i.name }

            if(v == null)
            {
                markAsMissing(i)
                continue
            }

            if(i.isFile)
            {
                if(v.isFile)
                {
                    if (!compareSingleFile(v, i))
                        markAsMissing(i)
                } else {
                    markAsMissing(i)
                }
            } else  {
                if(v.isFile)
                {
                    markAsMissing(i)
                } else {
                    findMissings(i.files, v.files)
                }
            }
        }
    }

    /**
     * 寻找多余的文件
     */
    private fun findRedundants(invariable: List<ComparableFile>, variable: List<ComparableFile>)
    {
        for (v in variable)
        {
            val i = invariable.firstOrNull { it.name == v.name }

            if(i != null)
            {
                if(!v.isFile && !i.isFile)
                    findRedundants(i.files, v.files)
            } else {
                markAsRedundant(v)
            }
        }
    }

    /**
     * 对比两个路径相同的文件是否一致
     * @param a 参与对比的a文件
     * @param b 参与对比的b文件
     */
    private fun compareSingleFile(a: ComparableFile, b: ComparableFile): Boolean
    {
        return a.modified == b.modified || b.hash == a.hash
    }

    /**
     * 将一个文件或者目录标记为多余
     */
    private fun markAsRedundant(file: ComparableFile)
    {
        if(!file.isFile)
        {
            for (f in file.files)
            {
                if(f.isFile)
                    redundantFiles += f.relativePath
                else
                    markAsRedundant(f)
            }

            redundantFolders += file.relativePath
        } else {
            redundantFiles += file.relativePath
        }
    }

    /**
     * 将一个文件或者目录标记为缺少
     */
    private fun markAsMissing(file: ComparableFile)
    {
        if (file.isFile)
        {
            missingFiles += file.relativePath
        } else {
            missingFolders += file.relativePath

            for (f in file.files)
                markAsMissing(f)
        }
    }

    /**
     * 检查文件移动
     */
    private fun detectFileMovings(invariable: List<ComparableFile>, variable: List<ComparableFile>)
    {
        fun get(list: List<ComparableFile>, path: String): ComparableFile
        {
            val index = path.indexOf("/")
            val rootName = if (index == -1) path else path.substring(0, index)
            val root = list.first { f -> f.name == rootName }

            if (index == -1)
                return root

            return root.find(path.substring(index + 1))!!
        }

        val hashCachesI = mutableMapOf<String, String>()
        val hashCachesV = mutableMapOf<String, String>()

        val redundantFileNames = redundantFiles.map { Pair(PathUtils.getFileNamePart(it), it) }
        val missingFileNames = missingFiles.map { Pair(PathUtils.getFileNamePart(it), it) }

        for (redundant in redundantFileNames)
        {
            for (missing in missingFileNames)
            {
                // 首先需要文件名相同
                if (redundant.first != missing.first)
                    continue

                val v = get(variable, redundant.second)
                val i = get(invariable, missing.second)
                val hashV = hashCachesV.getOrPut(redundant.second) { v.hash }
                val hashI = hashCachesI.getOrPut(missing.second) { i.hash }

                // 再者需要校验相同
                if (hashV == hashI)
                    moveFiles += Pair(v.relativePath, i.relativePath)
            }
        }

        // 把同时移动的文件当做普通文件处理，而非移动的文件
        val ambiguous = mutableListOf<String>()
        val appeareds = mutableListOf<String>()
        for (moving in moveFiles)
        {
            if (moving.first in appeareds && moving.first !in ambiguous)
                ambiguous.add(moving.first)

            appeareds.add(moving.first)
        }

        moveFiles.removeIf { ambiguous.any { a -> a == it.first } }

        missingFiles.removeIf { moveFiles.any { m -> m.second == it } }
        redundantFiles.removeIf { moveFiles.any { m -> m.first == it } }
    }

    override fun toString(): String = toString(null)

    fun toString(overwrites: OverwriteFile? = null): String
    {
        overwrites?.reload()

        return buildString {
            for (f in redundantFolders)
                append("旧目录: $f\n")

            for (f in missingFolders)
                append("新目录: $f\n")

            for (f in redundantFiles)
                append("旧文件: $f\n")

            for (f in missingFiles)
            {
                append("新文件: $f")

                if (overwrites != null && f in overwrites)
                    append(" （强制覆盖）")

                append("\n")
            }

            for ((from, to) in moveFiles)
                append("移动文件: $from => $to\n")
        }.trim()
    }
}