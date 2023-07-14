package mcpatch.interactive

import mcpatch.McPatchManage
import mcpatch.core.PatchFileReader
import mcpatch.exception.McPatchManagerException
import mcpatch.logging.Log
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.ByteArrayOutputStream

class Test
{
    fun execute()
    {
        Log.info("正在验证所有版本文件")

        for (version in McPatchManage.versionList.read())
        {
            val patchFile = McPatchManage.publicDir + "$version.mcpatch.zip"

            if (!patchFile.exists)
                throw McPatchManagerException("版本 ${patchFile.path} 的数据文件丢失或者不存在，验证未通过")

            Log.info("开始验证 $version 版本")

            val reader = PatchFileReader(version, ZipFile(patchFile.file, "utf-8"))

            val buf = ByteArrayOutputStream()

            for ((index, entry) in reader.withIndex())
            {
                Log.info("[$version] 验证文件(${index + 1}/${reader.meta.newFiles.size}): ${entry.meta.path}")

                buf.reset()
                entry.copyTo(buf)
            }
        }

        Log.info("所有版本文件验证已通过")
    }
}