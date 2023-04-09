package mcpatch

import mcpatch.core.Input
import mcpatch.core.VersionList
import mcpatch.exception.McPatchManagerException
import mcpatch.interactive.*
import mcpatch.utils.EnvironmentUtils
import mcpatch.utils.File2

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
        historyDir.mkdirs()
        workspaceDir.mkdirs()
        publicDir.mkdirs()

        println("McPatchManage v${EnvironmentUtils.version}")

        while (true)
        {
            println("主菜单: (输入字母执行命令)")
            println("  c: 创建新版本 (最新版本为 ${versionList.getNewest()} )")
            println("  t: 验证所有版本文件")
            println("  ?: 查看隐藏指令")
            println("  q: 退出")
            print("> ")
            System.out.flush()

            try {
                when(val input = Input.readAnyString())
                {
                    "c" -> Create().execute()
                    "t" -> Test().execute()
                    "?" -> {
                        println("隐藏指令：")
                        println("  combine: 合并历史更新包")
                        println("  restore: 还原工作空间目录(workspace)的修改")
                        println("  revert: 还原历史目录(history)的修改")
                        println("  clear: 删除所有历史版本")
                    }
                    "combine" -> Combine().execute()
                    "restore" -> Restore().execute()
                    "revert" -> Revert().execute()
                    "clear" -> Clear().execute()
                    "q" -> break
                    else -> {
                        if (input.isNotEmpty())
                        {
                            println("$input 不是一个命令")
                            Input.readAnyString()
                        } else {
                            println()
                        }
                        continue
                    }
                }
            } catch (e: McPatchManagerException) {
                println(e.message)
            }

            System.gc()
            Input.readAnyString()
        }

        println("结束运行")
        Thread.sleep(1500)
    }
}