package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.logging.Log
import java.util.*

class Status
{
    fun execute()
    {
        Log.info("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(McPatchManage.historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(history.files, workspace.files, McPatchManage.ignorefile, true)

        if (hasDiff)
        {
            Log.info("----------以下为文件修改列表（共 ${diff.totalDiff} 处文件改动）----------")
            Log.info(diff.toString(McPatchManage.overwritefile))
            Log.info("----------以上为文件修改列表（共 ${diff.totalDiff} 处文件改动）----------")
        } else {
            Log.info("没有任何文件改动")
        }
    }
}