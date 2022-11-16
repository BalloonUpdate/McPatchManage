package mcpatch

import mcpatch.core.VersionList
import mcpatch.extension.RuntimeExtension.usedMemory
import mcpatch.interactive.*
import mcpatch.utils.EnvironmentUtils
import mcpatch.utils.File2
import mcpatch.utils.HashUtils
import mcpatch.utils.MiscUtils

object McPatchManage
{
    val workdir = if (EnvironmentUtils.isPackaged) File2(System.getProperty("user.dir")) else File2("testdir")
    val workspaceDir = workdir + "workspace"
    val historyDir = workdir + "history"
    val publicDir = workdir + "public"
    val versionList = VersionList(workdir + "public/mc-patch-versions.txt")

    @JvmStatic
    fun main(args: Array<String>)
    {
        historyDir.mkdirs()
        workspaceDir.mkdirs()
        publicDir.mkdirs()

        println("McPatchManage v${EnvironmentUtils.version}")

        while (true)
        {
            versionList.reload()

            val ramUsed = MiscUtils.convertBytes(Runtime.getRuntime().usedMemory())
            val ramTotal = MiscUtils.convertBytes(Runtime.getRuntime().maxMemory())

            println("====================主菜单====================")
            println("1.创建新版本（RAM： $ramUsed / $ramTotal）")
            println("2.查看所有版本号（最新三个版本为: ${versionList.getNewest3()}）")
            println("3.检查文件修改状态")
            println("4.还原所有文件修改")
            println("q.退出 (输入序号+Enter来进行你想要的操作)")

            when(mcpatch.core.Input.readInputUntil("(\\d|q|ch|bv)", "有效的选择"))
            {
                "1" -> CreateVersion().loop()
                "2" -> ListVersion().loop()
                "3" -> CheckStatus().loop()
                "4" -> RevertWorkspace().loop()
                "bv" -> BacktrackVersion().loop()
                "ch" -> ClearHistory().loop()
                "q" -> break
                else -> break
            }

            System.gc()
        }

        println("结束运行")
        Thread.sleep(1500)
    }
}