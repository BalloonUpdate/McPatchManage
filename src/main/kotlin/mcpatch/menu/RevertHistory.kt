package mcpatch.menu

import mcpatch.McPatchManage
import mcpatch.core.PatchFileReader
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException
import mcpatch.classextension.FileExtension.bufferedOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File

class RevertHistory : BaseInteractive() {
    override fun run() {
        log.info("即将同时还原工作空间目录(workspace)和历史目录(history)下的内容")
        log.info("要继续吗？（输入y或者n）")

        if (!readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        log.info("请再次确认！（输入y或者n）")

        if (!readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        // 还原history目录
        for (version in McPatchManage.versionList.read())
        {
            val patchFile = File(McPatchManage.publicDir, "$version.mcpatch.zip")

            if (!patchFile.exists())
                throw McPatchManagerException("版本 ${patchFile.path} 的数据文件丢失或者不存在，版本还原失败")

            val reader = PatchFileReader(version, ZipFile(patchFile, "utf-8"))

            // 删除旧文件和旧目录，还有创建新目录
            reader.meta.oldFiles.map { File(McPatchManage.historyDir, it) }.forEach { it.delete() }
            reader.meta.oldFolders.map { File(McPatchManage.historyDir, it) }.forEach { it.delete() }
            reader.meta.newFolders.map { File(McPatchManage.historyDir, it) }.forEach { it.mkdirs() }

            for ((index, entry) in reader.withIndex())
            {
                log.info("[$version] 解压(${index + 1}/${reader.meta.newFiles.size}) ${entry.meta.path}")

                val file = File(McPatchManage.historyDir, entry.meta.path)

                file.bufferedOutputStream().use { stream -> entry.copyTo(stream) }
            }

            log.info("[$version] 版本 $version 处理完成")
        }

        // 同步workspace目录
        log.info("正在同步工作空间")

        McPatchManage.workspaceDir.delete()
        McPatchManage.workspaceDir.mkdirs()

        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(McPatchManage.historyDir)
        val diff = DirectoryDiff()
        diff.compare(workspace.files, history.files, McPatchManage.ignorefile, true)
        workspace.syncFrom(diff, McPatchManage.historyDir)

        log.info("所有目录已经还原！")
    }
}