package mcpatch.interactive

import com.lee.bsdiff.BsPatch
import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.McPatchManage.versionList
import mcpatch.data.ModificationMode
import mcpatch.data.VersionMetadata
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.extension.FileExtension.bufferedInputStream
import mcpatch.extension.FileExtension.bufferedOutputStream
import mcpatch.extension.StreamExtension.actuallySkip
import mcpatch.extension.StreamExtension.copyAmountTo
import mcpatch.utils.File2
import mcpatch.utils.HashUtils
import org.apache.tools.bzip2.CBZip2InputStream
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BacktrackVersion
{
    fun loop()
    {
        println("即将回溯workspace目录和history目录下的内容")

        println("确定要回溯吗？（输入y或者n）")
        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("回溯过程中断")
            return
        }

        println("此操作不可逆转，请再次确认！（输入y或者n）")
        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("回溯过程中断")
            return
        }

        // 还原history目录
        val backtrackDir = historyDir
        backtrackDir.delete()
        backtrackDir.mkdirs()

        versionList.reload()

        for (version in versionList.versions)
        {
            val metaFile = publicDir + "$version.mc-patch.json"
            val patchFile = publicDir + "$version.mc-patch.bin"

            if (!metaFile.exists)
            {
                println("${metaFile.path} 文件不存在，版本回溯失败")
                return
            }

            if (!patchFile.exists)
            {
                println("${patchFile.path} 文件不存在，版本回溯失败")
                return
            }

            val meta = VersionMetadata(JSONObject(metaFile.content))

            // 删除旧文件和旧目录，还有创建新目录
            meta.oldFiles.map { (backtrackDir + it) }.forEach { it.delete() }
            meta.oldFolders.map { (backtrackDir + it) }.forEach { it.delete() }
            meta.newFolders.map { (backtrackDir + it) }.forEach { it.mkdirs() }

            if (meta.newFiles.isNotEmpty())
                if (!applyPatch(meta, version, backtrackDir, patchFile))
                {
                    println("回溯失败，请勿再进行任何操作，请将此错误报告给开发者")
                    return
                }
        }

        // 同步workspace目录
        println("正在回溯workspace目录")
        val workspace = RealFile.CreateFromRealFile(McPatchManage.workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        diff.compare(workspace.files, history.files)
        workspace.applyDiff(diff, historyDir)
    }

    /**
     * 合并patch文件
     */
    private fun applyPatch(meta: VersionMetadata, version: String, dir: File2, patchFile: File2): Boolean
    {
        patchFile.file.bufferedInputStream().use { patch ->
            var pointer = 0L

            for ((index, newFile) in meta.newFiles.withIndex())
            {
                val file = dir + newFile.path

                println("[$version] 解压(${index + 1}/${meta.newFiles.size}) ${newFile.path}")

                when (newFile.mode)
                {
                    ModificationMode.Empty -> {
                        file.delete()
                        file.create()
                    }

                    ModificationMode.Fill -> {
                        // 如果本地文件被修改过，就跳过更新
                        if (file.exists && HashUtils.sha1(file.file) == newFile.newFileHash)
                            continue

                        file.create()
                        file.file.bufferedOutputStream().use { finalFile ->
                            patch.actuallySkip(newFile.blockOffset - pointer)

                            // 拿到解压好的原始数据
                            ByteArrayOutputStream().use { decompressed ->
                                val bzip = CBZip2InputStream(patch)
                                bzip.copyAmountTo(decompressed, 1024 * 1024, newFile.rawLength)

                                // 检查解压后的二进制块
                                if (HashUtils.sha1(decompressed.toByteArray()) != newFile.newFileHash)
                                {
                                    println("版本 $version 的补丁文件的 解压后的二进制数据(${newFile.path}) 已损坏")
                                    return false
                                }

                                decompressed.writeTo(finalFile)
                            }

                            pointer = newFile.blockOffset + newFile.blockLength
                        }
                    }

                    ModificationMode.Modify -> {
                        val notFound = !file.exists
                        val notMatched = if (notFound) false else HashUtils.sha1(file.file) != newFile.oldFileHash

                        if (notFound || notMatched)
                            break

                        val tempBinFile = file.parent + (file.name + ".mc-patch-temporal.bin")

                        // 将修补好的文件输出到临时文件里
                        file.file.bufferedInputStream().use { old ->
                            tempBinFile.file.bufferedOutputStream().use { tempFile ->
                                patch.skip(newFile.blockOffset - pointer)

                                // 拿到解压好的原始数据
                                ByteArrayOutputStream().use { decompressed ->
                                    val bzip = CBZip2InputStream(patch)
                                    bzip.copyAmountTo(decompressed, 1024 * 1024, newFile.rawLength)

                                    val decompressedBlock = decompressed.toByteArray()

                                    // 检查解压后的二进制块
                                    if (HashUtils.sha1(decompressedBlock) != newFile.patchFileHash)
                                    {
                                        println("版本 $version 的补丁文件的 解压后的二进制数据(${newFile.path}) 已损坏")
                                        return false
                                    }

                                    ByteArrayInputStream(decompressedBlock).use { patchStream ->
                                        BsPatch().bspatch(old, patchStream, tempFile, old.available(), newFile.rawLength.toInt())
                                    }
                                }

                                pointer = newFile.blockOffset + newFile.blockLength
                            }

                            // 检查合并后的文件
                            if (HashUtils.sha1(tempBinFile.file) != newFile.newFileHash)
                            {
                                println("版本 $version 的补丁文件的 合并后的文件数据(${newFile.path}) 已损坏")
                                return false
                            }
                        }

                        tempBinFile.copy(file)
                        tempBinFile.delete()
                    }
                }
            }
        }
        return true
    }
}