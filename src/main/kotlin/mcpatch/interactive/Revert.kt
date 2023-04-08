package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.McPatchManage.versionList
import mcpatch.core.PatchFileReader
import mcpatch.data.VersionData
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException
import mcpatch.extension.FileExtension.bufferedOutputStream
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class Revert
{
    fun loop()
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

            val reader = PatchFileReader(version, patchFile)

            // 删除旧文件和旧目录，还有创建新目录
            reader.meta.oldFiles.map { (historyDir + it) }.forEach { it.delete() }
            reader.meta.oldFolders.map { (historyDir + it) }.forEach { it.delete() }
            reader.meta.newFolders.map { (historyDir + it) }.forEach { it.mkdirs() }

            for ((index, entry) in reader.withIndex())
            {
                println("[$version] 解压(${index + 1}/${reader.meta.newFiles.size}) ${entry.newFile.path}")

                val file = historyDir + entry.newFile.path

                file.file.bufferedOutputStream().use { stream -> entry.copyTo(stream) }
            }
        }

        // 同步workspace目录
        println("正在同步工作空间")

        McPatchManage.workspaceDir.delete()
        McPatchManage.workspaceDir.mkdirs()

        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        diff.compare(from = history.files, to = workspace.files)
        workspace.applyDiff(diff, historyDir)

        println("所有目录已经还原！")
    }
}