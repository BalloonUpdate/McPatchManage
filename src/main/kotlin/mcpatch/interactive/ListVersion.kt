package mcpatch.interactive

import mcpatch.McPatchManage.versionList

class ListVersion
{
    fun loop()
    {
        versionList.reload()

        println("===============当前有 ${versionList.versions.size} 个版本===============")
        println(versionList.versions.joinToString("\n"))
        println("===============当前有 ${versionList.versions.size} 个版本===============")
    }
}