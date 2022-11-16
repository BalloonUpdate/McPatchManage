package mcpatch.interactive

import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir

class ClearHistory
{
    fun loop()
    {
        println("即将清理所有历史版本和history目录下的内容")

        println("确定要清理所有文件吗？（输入y或者n）")
        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("清理过程中断")
            return
        }

        println("此操作不可逆转，请再次确认！（输入y或者n）")
        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("清理过程中断")
            return
        }

        println("正在清理历史")

        historyDir.delete()
        historyDir.mkdirs()

        publicDir.delete()
        publicDir.mkdirs()

        println("所有历史版本和history目录已清理")
    }
}