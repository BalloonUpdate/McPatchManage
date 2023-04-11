package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.McPatchManage.versionList
import mcpatch.core.PatchFileReader
import mcpatch.data.ModificationMode
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException
import mcpatch.extension.FileExtension.bufferedOutputStream
import mcpatch.extension.StreamExtension.copyAmountTo
import mcpatch.stream.MemoryOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel

class Revert
{
    fun execute()
    {
        println("即将同时还原工作空间目录(workspace)和历史目录(history)下的内容")
        println("要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        println("请再次确认！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        // 还原history目录
        for (version in versionList.read())
        {
            val patchFile = publicDir + "$version.mcpatch.zip"

            if (!patchFile.exists)
                throw McPatchManagerException("版本 ${patchFile.path} 的数据文件丢失或者不存在，版本还原失败")

            val reader = PatchFileReader(version, ZipFile(patchFile.file, "utf-8"))

            // 删除旧文件和旧目录，还有创建新目录
            reader.meta.oldFiles.map { (historyDir + it) }.forEach { it.delete() }
            reader.meta.oldFolders.map { (historyDir + it) }.forEach { it.delete() }
            reader.meta.newFolders.map { (historyDir + it) }.forEach { it.mkdirs() }

            for ((index, entry) in reader.withIndex())
            {
                println("[$version] 解压(${index + 1}/${reader.meta.newFiles.size}) ${entry.meta.path}")

                val file = historyDir + entry.meta.path

                if (entry.mode == ModificationMode.ZipModify)
                {
                    val stream = entry.getInputStream()
                    val mem = MemoryOutputStream(entry.meta.rawLength.toInt())
                    stream.copyAmountTo(mem, entry.meta.rawLength)
                    val reader2 = PatchFileReader(version, ZipFile(SeekableInMemoryByteChannel(mem.buffer())))
                    reader2.meta.oldFiles.map { (historyDir + it) }.forEach { it.delete() }
                    reader2.meta.oldFolders.map { (historyDir + it) }.forEach { it.delete() }
                    reader2.meta.newFolders.map { (historyDir + it) }.forEach { it.mkdirs() }

                    // 上次开发进度推进到这里。

                    continue
                } else {
                    file.file.bufferedOutputStream().use { stream -> entry.copyTo(stream) }
                }
            }

            println("[$version] 版本 $version 处理完成")
        }

        // 同步workspace目录
        println("正在同步工作空间")

        McPatchManage.workspaceDir.delete()
        McPatchManage.workspaceDir.mkdirs()

        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        diff.compare(workspace.files, history.files, true)
        workspace.syncFrom(diff, historyDir)

        println("所有目录已经还原！")
    }
}