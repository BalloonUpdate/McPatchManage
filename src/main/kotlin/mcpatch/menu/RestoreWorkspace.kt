package mcpatch.menu

import mcpatch.McPatchManage
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException

class RestoreWorkspace : BaseInteractive() {
    override fun run() {
        log.info("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(McPatchManage.historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(workspace.files, history.files, McPatchManage.ignorefile, true)

        if (hasDiff)
        {
            log.info("----------即将还原以下所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
            log.info(diff.toString(McPatchManage.overwritefile))
            log.info("----------即将还原以上所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
        } else {
            log.info("工作空间目录(workspace)没有任何改动，不需要还原")
            return
        }

        log.info("即将还原工作空间目录(workspace)里的所有文件修改")
        log.info("要继续吗？（输入y或者n）")

        if (!readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        log.info("请再次确认！（输入y或者n）")

        if (!readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        log.info("正在还原文件修改，可能需要一点时间")

        workspace.syncFrom(diff, McPatchManage.historyDir)

        log.info("所有文件修改已还原！")
    }
}