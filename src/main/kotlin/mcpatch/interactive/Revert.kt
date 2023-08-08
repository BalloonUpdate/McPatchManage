package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.McPatchManage.versionList
import mcpatch.McPatchManage.workspaceDir
import mcpatch.core.PatchFileReader
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException
import mcpatch.extension.FileExtension.bufferedOutputStream
import mcpatch.logging.Log
import org.apache.commons.compress.archivers.zip.ZipFile

class Revert
{
    fun execute()
    {
        Log.info("即将同时还原工作空间目录(workspace)和历史目录(history)下的内容")
        Log.info("要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        Log.info("请再次确认！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        workspaceDir.clear()
        historyDir.clear()

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
                Log.info("[$version] 解压(${index + 1}/${reader.meta.newFiles.size}) ${entry.meta.path}")

                val file = historyDir + entry.meta.path

                file.parent.mkdirs()

                file.file.bufferedOutputStream().use { stream -> entry.copyTo(stream) }
            }

            Log.info("[$version] 版本 $version 处理完成")
        }

        // 同步workspace目录
        Log.info("正在同步工作空间")

        McPatchManage.workspaceDir.delete()
        McPatchManage.workspaceDir.mkdirs()

        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        diff.compare(workspace.files, history.files, McPatchManage.ignorefile, true)
        workspace.syncFrom(diff, historyDir)

        Log.info("所有目录已经还原！")
    }
}