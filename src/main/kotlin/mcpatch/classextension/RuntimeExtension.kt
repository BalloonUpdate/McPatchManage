package mcpatch.classextension

object RuntimeExtension
{
    fun Runtime.usedMemory(): Long
    {
        return totalMemory() - freeMemory()
    }
}