package mcpatch.menu

import mcpatch.McPatchManage

class MainMenu : BaseInteractive() {
    override fun run() {
        printHelp()


    }

    private fun printHelp() {
        log.info("主菜单: (输入字母执行命令)")
        log.info("  c: 创建新版本 (最新版本为 ${McPatchManage.versionList.getNewest()} )")
        log.info("  s: 检查文件修改状态")
        log.info("  t: 验证所有版本文件")
        log.info("  combine: 合并历史更新包")
        log.info("  restore: 还原工作空间目录(workspace)的修改")
        log.info("  revert: 还原历史目录(history)的修改")
        log.info("  clear: 删除所有历史版本")
        log.info("  q: 退出")
        log.info("> ", false)
        System.out.flush()
    }
}