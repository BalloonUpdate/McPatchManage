package mcpatch.core

import java.util.*

object Input
{
    /**
     * 从命令行读取一段符合要求的文字输入
     * @param reg 正则表达式
     * @return [是否符合要求, 输入的内容]
     */
    fun readInput(reg: String): Pair<Boolean, String>
    {
        System.`in`.skip(System.`in`.available().toLong())

        while (true)
        {
            val input = (readlnOrNull() ?: continue).trim()

            if (reg.isEmpty() || Regex(reg).matches(input))
                return Pair(true, input)

            return Pair(false, input)
        }
    }

    /**
     * 从命令行读取一段符合要求的文字输入，直到符合要求为止
     * @param reg 正则表达式
     * @param desc 输入内容描述
     * @return 读取到的内容
     */
    fun readInputUntil(reg: String, desc: String): String
    {
        while (true)
        {
            val input = readInput(reg)

            if (input.first)
                return input.second

            if (desc.isNotEmpty())
                println("输入的 <${input.second}> 不是 $desc")
        }
    }

    /**
     * 从命令行读取任意一行字符串
     */
    fun readAnyString(): String
    {
        return readInputUntil(".*", "")
    }

    /**
     * 从命令行等待Enter
     */
    fun waitForEnterPress()
    {
        readInput("")
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
}