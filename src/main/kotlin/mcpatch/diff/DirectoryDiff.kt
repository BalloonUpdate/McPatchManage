package mcpatch.diff

/**
 * 目录差异对比
 */
class DirectoryDiff
{
    val oldFolders: MutableList<String> = mutableListOf()
    val oldFiles: MutableList<String> = mutableListOf()
    val newFolders: MutableList<String> = mutableListOf()
    val newFiles: MutableList<String> = mutableListOf()

    /**
     * 总共有多少个文件变动
     */
    var totalDiff: Int = 0

    /**
     * 对比文件差异
     * @param source 参照目录
     * @param target 被对比目录
     * @return 有无差异
     */
    fun compare(source: List<ComparableFile>, target: List<ComparableFile>): Boolean
    {
        findNews(source, target)
        findOlds(source, target)

        totalDiff = oldFolders.size + oldFiles.size + newFolders.size + newFiles.size
        return totalDiff > 0
    }

    /** 扫描需要下载的文件(不包括被删除的)
     * @param source 参照目录
     * @param target 被对比目录
     */
    private fun findNews(source: List<ComparableFile>, target: List<ComparableFile>) {
        for (c in source)
        {
            val corresponding = target.firstOrNull { it.name == c.name } // 此文件可能不存在

            if(corresponding == null) // 如果文件不存在的话，就不用校验了，可以直接进行下载
            {
                markAsNew(c)
                continue
            }

            if(c.isFile)
            {
                if(corresponding.isFile)
                {
                    if (!compareSingleFile(corresponding, c))
                    {
//                        markAsOld(corresponding)
                        markAsNew(c)
                    }
                } else {
//                    markAsOld(corresponding)
                    markAsNew(c)
                }
            } else  {
                if(corresponding.isFile)
                {
//                    markAsOld(corresponding)
                    markAsNew(c)
                } else {
                    findNews(c.files, corresponding.files)
                }
            }
        }
    }

    /** 扫描需要删除的文件
     * @param source 参照目录
     * @param target 被对比目录
     */
    private fun findOlds(source: List<ComparableFile>, target: List<ComparableFile>)
    {
        for (f in target)
        {
            val corresponding = source.firstOrNull { it.name == f.name }

            if(corresponding != null)
            {
                if(!f.isFile && !corresponding.isFile)
                    findOlds(corresponding.files, f.files)
            } else {
                markAsOld(f)
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
     * 将一个文件文件或者目录标记为旧文件
     * @param old 被标记的文件或者目录
     */
    private fun markAsOld(old: ComparableFile)
    {
        if(!old.isFile)
        {
            for (f in old.files)
            {
                if(f.isFile)
                    oldFiles += f.relativePath
                else
                    markAsOld(f)
            }

            oldFolders += old.relativePath
        } else {
            oldFiles += old.relativePath
        }
    }

    /**
     * 将一个文件文件或者目录标记为新文件
     * @param new 被标记的文件或者目录
     */
    private fun markAsNew(new: ComparableFile)
    {
        if (new.isFile)
        {
            newFiles += new.relativePath
        } else {
            newFolders += new.relativePath
            for (n in new.files)
                markAsNew(n)
        }
    }

    /**
     * 输出到String
     * @param oldFolder 旧目录的标签
     * @param newFolder 新目录的标签
     * @param oldFile 旧文件的标签
     * @param newFile 新文件的标签
     * @param lineSeparator 行间空隙
     */
    fun toString(
        oldFolder: String,
        newFolder: String,
        oldFile: String,
        newFile: String,
        lineSeparator: String = "\n"
    ): String {
        return buildString {
            for (f in oldFolders)
                append("$oldFolder: $f\n")

            if (oldFolders.isNotEmpty() && newFolders.isNotEmpty())
                append(lineSeparator)

            for (f in newFolders)
                append("$newFolder: $f\n")

            if (newFolders.isNotEmpty() && oldFiles.isNotEmpty())
                append(lineSeparator)

            for (f in oldFiles)
                append("$oldFile: $f\n")

            if (oldFiles.isNotEmpty() && newFiles.isNotEmpty())
                append(lineSeparator)

            for (f in newFiles)
                append("$newFile: $f\n")
        }.trim()
    }

    override fun toString(): String
    {
        return toString("旧目录", "新目录", "旧文件", "新文件")
    }
}