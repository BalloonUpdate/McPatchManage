package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.core.PatchFileReader
import mcpatch.exception.McPatchManagerException
import java.io.ByteArrayOutputStream

class Test
{
    fun loop()
    {
        println("正在验证所有版本文件")

        for (version in McPatchManage.versionList.read())
        {
            val patchFile = McPatchManage.publicDir + "$version.mcpatch.zip"

            if (!patchFile.exists)
                throw McPatchManagerException("版本 ${patchFile.path} 的数据文件丢失或者不存在，验证未通过")

            println("开始验证 $version 版本")

            val reader = PatchFileReader(version, patchFile)

            val buf = ByteArrayOutputStream()

            for ((index, entry) in reader.withIndex())
            {
                println("[$version] 验证文件(${index + 1}/${reader.meta.newFiles.size}): ${entry.newFile.path}")

                buf.reset()
                entry.read(buf)
            }
        }

        println("所有版本文件验证已通过")
    }
}