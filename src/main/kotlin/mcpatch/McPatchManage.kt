package mcpatch

import mcpatch.core.Input
import mcpatch.core.VersionList
import mcpatch.exception.McPatchManagerException
import mcpatch.interactive.*
import mcpatch.utils.EnvironmentUtils
import mcpatch.utils.File2
import java.util.concurrent.LinkedTransferQueue
import kotlin.system.exitProcess

object McPatchManage
{
    val workdir = if (EnvironmentUtils.isPackaged) File2(System.getProperty("user.dir")) else File2("testdir")
    val workspaceDir = workdir + "workspace"
    val historyDir = workdir + "history"
    val publicDir = workdir + "public"
    val combineDir = workdir + "combining"
    val versionList = VersionList(workdir + "public/versions.txt")

    @JvmStatic
    fun main(args: Array<String>)
    {
        var exitCode = 0

        Input.initInput(args)

        historyDir.mkdirs()
        workspaceDir.mkdirs()
        publicDir.mkdirs()

        println("McPatchManage v${EnvironmentUtils.version}")

        fun printHelp()
        {
            println("主菜单: (输入字母执行命令)")
            println("  c: 创建新版本 (最新版本为 ${versionList.getNewest()} )")
            println("  s: 检查文件修改状态")
            println("  ?: 查看隐藏命令")
            println("  q: 退出")
            print("> ")
            System.out.flush()
        }

        var noMenu = false

        while (true)
        {
            if (!Input.hasInput() && !noMenu)
                printHelp()

            try {
                noMenu = true

                when(val input = Input.readAnyString())
                {
                    "c" -> Create().execute()
                    "s" -> Status().execute()
                    "?" -> {
                        println("隐藏命令：")
                        println("  t: 验证所有版本文件")
                        println("  combine: 合并历史更新包")
                        println("  restore: 还原工作空间目录(workspace)的修改")
                        println("  revert: 还原历史目录(history)的修改")
                        println("  clear: 删除所有历史版本")
                    }
                    "combine" -> Combine().execute()
                    "restore" -> Restore().execute()
                    "revert" -> Revert().execute()
                    "clear" -> Clear().execute()
                    "t" -> Test().execute()
                    "q" -> break
                    else -> {
                        if (input.isNotEmpty())
                        {
                            println("$input 不是一个命令")
                        } else {
                            noMenu = false
                        }
                    }
                }

                exitCode = 0
            } catch (e: McPatchManagerException) {
                exitCode = 1
                println(e.message)
            }
        }

        exitProcess(exitCode)
    }
}