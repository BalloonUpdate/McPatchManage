package mcpatch.interactive

import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.workspaceDir
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile

class CheckStatus
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
            println("----------以下为文件修改列表（共 ${diff.totalDiff} 处文件变动）----------")
            println(diff)
            println("----------以上为文件修改列表（共 ${diff.totalDiff} 处文件变动）----------")
        } else {
            println("workspace目录没有任何改动")
        }
    }
}