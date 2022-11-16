package mcpatch.interactive

import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.workspaceDir
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile

class RevertWorkspace
{
    fun loop()
    {
        println("正在计算文件修改，可能需要一点时间")
        val workspace = RealFile.CreateFromRealFile(workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(workspace.files, history.files)

        if (hasDiff)
        {
            println("----------即将还原以下所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
            println(diff)
            println("----------即将还原以上所有文件修改（共 ${diff.totalDiff} 处文件变动）----------")
        } else {
            println("workspace目录没有任何改动")
            return
        }

        println("确定要还原所有文件修改吗？（输入y或者n）")
        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("还原文件修改过程中断")
            return
        }

        println("此操作不可撤销，请再次确认！（输入y或者n）")
        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("还原文件修改过程中断")
            return
        }

        println("正在还原文件修改，可能需要一点时间")
        workspace.applyDiff(diff, historyDir)
        println("所有文件修改已还原")
    }
}