package mcpatch.interactive

import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.exception.McPatchManagerException

class Clear
{
    fun loop()
    {
        println("即将清理以下目录下的所有内容：")
        println("1. 工作空间目录(workspace)")
        println("2. 历史目录(history)")
        println("3. 公共目录(public)")
        println("清理后将删除所有数据，并还原到重新安装时最干净的状态")
        println("此操作非常危险，仅供调试使用！确定要继续吗？（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        println("请再次确认操作无误！（输入y或者n）")

        if (!mcpatch.core.Input.readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        println("请最终确认！（输入y或者n）")

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