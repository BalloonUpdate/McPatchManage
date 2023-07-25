package mcpatch.menu

import mcpatch.InteractiveExecutor
import mcpatch.utility.CapturedLogging
import java.util.*
import java.util.concurrent.LinkedTransferQueue

abstract class BaseInteractive : InteractiveExecutor.Task() {



    private val inputBuf = LinkedTransferQueue<String>()
    private val lineBuf = StringBuilder()


    /**
     * 从命令行读取一段符合要求的文字输入
     * @param reg 正则表达式
     * @return [是否符合要求, 输入的内容]
     */
    fun readInput(reg: String?): Pair<Boolean, String>
    {
        while (true)
        {
            read()
            val input = inputBuf.poll()!!

            if (reg == null || Regex(reg).matches(input))
                return Pair(true, input)

            return Pair(false, input)
        }
    }

    /**
     * 读取一个Y或者N输入
     * @param default 默认值
     * @return Yes 或者 No
     */
    fun readYesOrNot(default: Boolean): Boolean
    {
        val (result, input) = readInput("[yYnN]?")

        if (!result)
            return default

        val choice = input.lowercase(Locale.getDefault())

        if (choice.isEmpty())
            return default

        return choice == "y"
    }

    /**
     * 从控制台读取输入，如果buf为空则此方法会阻塞
     */
    private fun read()
    {
        val block = inputBuf.isEmpty()

        if (!block && System.`in`.available() == 0)
            return

        fun readOne()
        {
            val d = System.`in`.read()

            if (d == -1)
                return

            val chr = d.toChar()
            val isLineEnd = chr == '\n' || chr == '\r' || chr == ' '

            if (isLineEnd)
            {
                if (lineBuf.isNotEmpty())
                {
                    inputBuf.add(lineBuf.toString())
                    lineBuf.clear()
                }
            } else {
                lineBuf.append(chr)
            }
        }

        if (block)
        {
            while (inputBuf.isEmpty())
                readOne()
        } else {
            for (i in 0 until System.`in`.available())
                readOne()
        }
    }
}