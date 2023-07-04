package mcpatch.core

import com.hrakaroo.glob.GlobPattern
import com.hrakaroo.glob.MatchingEngine
import mcpatch.utils.File2

abstract class AbstractRulingFile(val file: File2)
{
    private val globs: MutableList<MatchingEngine> = mutableListOf()

    init {
        reload()
    }

    fun reload()
    {
        val lines = if (file.exists) file.file.readLines().filter { it.trim().isNotEmpty() } else listOf()
        val compileds = lines.map { GlobPattern.compile(it) }

        globs.clear()
        globs.addAll(compileds)
    }

    fun match(path: String): Boolean = globs.any { it.matches(path) }

    operator fun contains(path: String) = match(path)
}