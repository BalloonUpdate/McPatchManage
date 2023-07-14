package mcpatch

import mcpatch.core.IgnoreFile
import mcpatch.core.Input
import mcpatch.core.OverwriteFile
import mcpatch.core.VersionList
import mcpatch.exception.McPatchManagerException
import mcpatch.interactive.*
import mcpatch.logging.ConsoleHandler
import mcpatch.logging.FileHandler
import mcpatch.logging.Log
import mcpatch.utils.EnvironmentUtils
import mcpatch.utils.File2
import kotlin.system.exitProcess

object McPatchManage
{
    val workdir = if (EnvironmentUtils.isPackaged) File2(System.getProperty("user.dir")) else File2("testdir")
    val workspaceDir = workdir + "workspace"
    val historyDir = workdir + "history"
    val publicDir = workdir + "public"
    val combineDir = workdir + "combining"
    val commandDir = workdir + "command"
    val versionList = VersionList(workdir + "public/versions.txt")
    val ignorefile = IgnoreFile(workdir + "ignores.txt")
    val overwritefile = OverwriteFile(workdir + "overwrites.txt")

    @JvmStatic
    fun main(args: Array<String>)
    {
        Log.addHandler(ConsoleHandler(Log, Log.LogLevel.ALL))

        if (args.firstOrNull() == "serve")
        {
            servingLoop()
            return
        }

        val exitCode = run(args.toMutableList())
        exitProcess(exitCode)
    }

    fun run(args: MutableList<String>): Int
    {
        // 防止忘记退出
        if (args.isNotEmpty() && args.last() != "q" )
            args.add("q")

        var exitCode = 0

        Input.initInput(args)

        historyDir.mkdirs()
        workspaceDir.mkdirs()
        publicDir.mkdirs()

        Log.info("McPatchManage v${EnvironmentUtils.version}")

        fun printHelp()
        {
            Log.info("主菜单: (输入字母执行命令)")
            Log.info("  c: 创建新版本 (最新版本为 ${versionList.getNewest()} )")
            Log.info("  s: 检查文件修改状态")
            Log.info("  ?: 查看隐藏命令")
            Log.info("  q: 退出")
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
                        Log.info("隐藏命令：")
                        Log.info("  t: 验证所有版本文件")
                        Log.info("  combine: 合并历史更新包")
                        Log.info("  restore: 还原工作空间目录(workspace)的修改")
                        Log.info("  revert: 还原历史目录(history)的修改")
                        Log.info("  clear: 删除所有历史版本")
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
                            Log.info("$input 不是一个命令")
                        } else {
                            noMenu = false
                        }
                    }
                }

                exitCode = 0
            } catch (e: McPatchManagerException) {
                exitCode = 1
                Log.info(e.message ?: "No Exception Message")
            }
        }

        return exitCode
    }

    fun servingLoop()
    {
        Log.info("Run in serve mode")

        commandDir.mkdirs()

        while (true)
        {
            val commandFiles = commandDir.files.filter { it.name.startsWith("+") && it.name.endsWith(".txt") }

            for (file in commandFiles)
            {
                val cmd = file.name.substring(1, file.name.length - 4)
                val runningFile = file.parent + "running-$cmd.txt"
                val doneFile = file.parent + "$cmd.txt"

                file.move(runningFile)

                val loggingHanler = FileHandler(Log, runningFile)
                Log.addHandler(loggingHanler)

                Log.info("从文件运行命令: $cmd")

                val startTime = System.currentTimeMillis()

                val exitCode = run(cmd.split(" ").toMutableList())

                val elapse = System.currentTimeMillis() - startTime

                Log.info("命令运行结束，执行时间：${elapse / 1000f}毫秒，退出代码：$exitCode")

                // 等待日志写入完毕
                Thread.sleep(300)

                Log.removeHandler(loggingHanler)

                runningFile.move(doneFile)
            }

            Thread.sleep(1000)
        }
    }
}