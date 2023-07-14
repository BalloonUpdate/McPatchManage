package mcpatch.interactive

import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.exception.McPatchManagerException
import mcpatch.logging.Log

class Clear
{
    fun execute()
    {
        Log.info("即将清理历史目录(history)和公共目录(public)，但不会清理工作空间目录(workspace)")
        Log.info("此操作非常危险，仅供调试使用！确定要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        Log.info("请再次确认操作无误！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        Log.info("正在清理数据...")

        historyDir.delete()
        historyDir.mkdirs()

        publicDir.delete()
        publicDir.mkdirs()

        Log.info("所有数据已经清理")
    }
}