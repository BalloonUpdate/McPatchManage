package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.workspaceDir
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.exception.McPatchManagerException

class Restore
{
    fun execute()
    {
        println("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(workspace.files, history.files, McPatchManage.ignorefile, true)

        if (hasDiff)
        {
            println("----------即将还原以下所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
            println(diff.toString(McPatchManage.overwritefile))
            println("----------即将还原以上所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
        } else {
            println("工作空间目录(workspace)没有任何改动，不需要还原")
            return
        }

        println("即将还原工作空间目录(workspace)里的所有文件修改")
        println("要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        println("请再次确认！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("还原过程中断")

        println("正在还原文件修改，可能需要一点时间")

        workspace.syncFrom(diff, historyDir)

        println("所有文件修改已还原！")
    }
}