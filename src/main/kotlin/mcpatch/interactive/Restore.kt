package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.workspaceDir
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException
import mcpatch.logging.Log

class Restore
{
    fun execute()
    {
        Log.info("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(workspace.files, history.files, McPatchManage.ignorefile, true)

        if (hasDiff)
        {
            Log.info("----------即将还原以下所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
            Log.info(diff.toString(McPatchManage.overwritefile))
            Log.info("----------即将还原以上所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
        } else {
            Log.info("工作空间目录(workspace)没有任何改动，不需要还原")
            return
        }

        Log.info("即将还原工作空间目录(workspace)里的所有文件修改")
        Log.info("要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        Log.info("请再次确认！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        Log.info("正在还原文件修改，可能需要一点时间")

        workspace.syncFrom(diff, historyDir)

        Log.info("所有文件修改已还原！")
    }
}