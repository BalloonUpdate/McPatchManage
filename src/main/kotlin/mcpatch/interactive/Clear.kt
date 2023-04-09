package mcpatch.interactive

import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.exception.McPatchManagerException

class Clear
{
    fun execute()
    {
        println("即将清理历史目录(history)和公共目录(public)，但不会清理工作空间目录(workspace)")
        println("此操作非常危险，仅供调试使用！确定要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        println("请再次确认操作无误！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        println("正在清理数据...")

        historyDir.delete()
        historyDir.mkdirs()

        publicDir.delete()
        publicDir.mkdirs()

        println("所有数据已经清理")
    }
}