package mcpatch.menu

import mcpatch.McPatchManage
import mcpatch.exception.McPatchManagerException

class ClearAllData : BaseInteractive() {
    override fun run() {
        log.info("即将清理历史目录(history)和公共目录(public)，但不会清理工作空间目录(workspace)")
        log.info("此操作非常危险，仅供调试使用！确定要继续吗？（输入y或者n）")

        if (!readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        log.info("请再次确认操作无误！（输入y或者n）")

        if (!readYesOrNot(false))
            throw McPatchManagerException("清理过程中断")

        log.info("正在清理数据...")

        McPatchManage.historyDir.delete()
        McPatchManage.historyDir.mkdirs()

        McPatchManage.publicDir.delete()
        McPatchManage.publicDir.mkdirs()

        log.info("所有数据已经清理")
    }
}