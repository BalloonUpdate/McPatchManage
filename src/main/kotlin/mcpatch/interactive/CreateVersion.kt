package mcpatch.interactive

import com.lee.bsdiff.BsDiff
import mcpatch.editor.ExternalTextFileEditor
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.McPatchManage.versionList
import mcpatch.McPatchManage.workdir
import mcpatch.McPatchManage.workspaceDir
import mcpatch.data.ModificationMode
import mcpatch.data.NewFile
import mcpatch.data.VersionMetadata
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.extension.FileExtension.bufferedInputStream
import mcpatch.extension.FileExtension.bufferedOutputStream
import mcpatch.extension.RuntimeExtension.usedMemory
import mcpatch.extension.StreamExtension.actuallySkip
import mcpatch.extension.StreamExtension.copyAmountTo
import mcpatch.utils.File2
import mcpatch.utils.HashUtils
import mcpatch.utils.MiscUtils
import org.apache.tools.bzip2.CBZip2InputStream
import org.apache.tools.bzip2.CBZip2OutputStream
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

class CreateVersion
{
    /**
     * 创建版本
     * @param version 新版本名称
     * @param metaFile meta文件
     * @param patchFile patch文件
     * @param editor 更新记录编辑器
     * @return 是否创建成功
     */
    private fun create(version: String, metaFile: File2, patchFile: File2, editor: ExternalTextFileEditor): Boolean
    {
        println("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(workspace.files, history.files)

        val tempPatchFile = patchFile.parent + "patch-temporal.bin"

        if (hasDiff)
        {
            println("----------以下为文件修改列表（共 ${diff.totalDiff} 处文件变动）----------")
            println(diff)
            println("----------以上为文件修改列表（共 ${diff.totalDiff} 处文件变动）----------")
        } else {
            println("注意，$version 是一个不包含任何更改的空版本")
        }

        // 计算预计内存消耗
        val totalMemory = (diff.newFiles.sumOf {
            val old = historyDir + it
            val new = workspaceDir + it
            val newLen = if (new.exists) new.length else 0
            val oldLen = if (old.exists) old.length else 0

            newLen + oldLen + (oldLen * 8)
        } * 1.3f).toLong() + (Runtime.getRuntime().usedMemory() * 1.2f).toLong()

        val ramRequired = MiscUtils.convertBytes(totalMemory)
        val ramAvaliable = MiscUtils.convertBytes(Runtime.getRuntime().maxMemory())
        println("打包过程预计消耗内存: $ramRequired, JVM总可用内存为: $ramAvaliable")
        println()
        println("确定要创建版本 $version 吗? （输入y或者n）如果有更新记录请粘贴到changelogs.txt文件里")

        if (totalMemory > Runtime.getRuntime().maxMemory())
            println("可用内存不足，打包过程可能发生崩溃，请使用JVM参数Xmx调整最大可用内存")

        if (!mcpatch.core.Input.readYesOrNot(false))
        {
            println("创建版本 $version 过程中断")
            return false
        }

        val start = System.currentTimeMillis()
        println("正在创建版本 $version 可能需要一点时间")

        // 创建版本记录文件
        val versionMeta = VersionMetadata()

        // 准备patch文件
        if (hasDiff)
        {
            if (diff.newFiles.isNotEmpty())
            {
                ByteArrayOutputStream().use { sharedBuf ->
                    tempPatchFile.file.bufferedOutputStream(8 * 1024 * 1024).use { patch ->
                        var wrote: Long = 0

                        for ((index, newFile) in diff.newFiles.withIndex())
                        {
                            val old = historyDir + newFile
                            val new = workspaceDir + newFile
                            val newLen = if (new.exists) new.length else 0
                            val oldLen = if (old.exists) old.length else 0
                            val case = old.name != new.name && old.name.equals(new.name, ignoreCase = true)
                            val mode = when {
                                newLen == 0L -> ModificationMode.Empty
                                (oldLen == 0L && newLen > 0) || case -> ModificationMode.Fill
                                else -> ModificationMode.Modify
                            }

                            if (max(oldLen, newLen) > Int.MAX_VALUE.toLong() - 1)
                            {
                                println("暂时不支持打包大小超过2GB的文件 $newFile")
                                return false
                            }

                            println("打包文件(${index + 1}/${diff.newFiles.size}) $newFile")

                            // 写入辅助头
                            val separator = ByteArray(128).also { it.fill('.'.code.toByte()) }
                            val comment = "<$newFile|$mode>".encodeToByteArray()
                            patch.write(separator)
                            patch.write(comment)
                            wrote += separator.size + comment.size

                            val offset = wrote

                            sharedBuf.reset()

                            when (mode)
                            {
                                ModificationMode.Fill -> {
                                    new.file.bufferedInputStream().use { stream ->
                                        // 压缩
                                        val bzip = CBZip2OutputStream(sharedBuf, 1)
                                        stream.copyTo(bzip, 1024 * 1024)
                                        bzip.finish()
                                        bzip.flush()

                                        // 不压缩
                                        // stream.copyTo(sharedTemp, 1024 * 1024)

                                        // 写进patch
                                        sharedBuf.writeTo(patch)
                                        wrote += sharedBuf.size()

                                        versionMeta.newFiles += NewFile(
                                            path = newFile,
                                            mode = mode,
                                            oldFileHash = "",
                                            newFileHash = HashUtils.sha1(new.file),
                                            patchFileHash = "",
                                            blockHash = HashUtils.sha1(sharedBuf.toByteArray()),
                                            blockOffset = offset,
                                            blockLength = sharedBuf.size().toLong(),
                                            rawLength = new.file.length()
                                        )
                                    }
                                }

                                ModificationMode.Empty -> {
                                    versionMeta.newFiles += NewFile(newFile, mode, "", "", "", "", 0, 0, 0)
                                }

                                ModificationMode.Modify -> { // 计算差异
                                    new.file.bufferedInputStream().use { ni ->
                                        old.file.bufferedInputStream().use { oi ->
                                            // 压缩
                                            val bzip = CBZip2OutputStream(sharedBuf)
                                            val patchLength: Long
                                            val patchFileHash: String
                                            ByteArrayOutputStream().use { temp ->
                                                try {
                                                    patchLength = BsDiff().bsdiff(oi, ni, temp, oi.available(), ni.available())
                                                } catch (e: OutOfMemoryError) {
                                                    println()
                                                    println("内存不足，打包过程中断！请尝试给JVM分配更多内存")
                                                    println()
                                                    return false
                                                }

                                                // 计算patch文件hash
                                                patchFileHash = HashUtils.sha1(temp.toByteArray())
                                                temp.writeTo(bzip)
                                            }
                                            bzip.finish()
                                            bzip.flush()

                                            // 不压缩
                                            // val wrote = BsDiff().bsdiff(oi, ni, sharedBuf, oi.available(), ni.available())

                                            // 写进patch
                                            sharedBuf.writeTo(patch)
                                            wrote += sharedBuf.size()

                                            versionMeta.newFiles += NewFile(
                                                path = newFile,
                                                mode = mode,
                                                oldFileHash = HashUtils.sha1(old.file),
                                                newFileHash = HashUtils.sha1(new.file),
                                                patchFileHash = patchFileHash,
                                                blockHash = HashUtils.sha1(sharedBuf.toByteArray()),
                                                blockOffset = offset,
                                                blockLength = sharedBuf.size().toLong(),
                                                rawLength = patchLength
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (tempPatchFile.length > Int.MAX_VALUE)
                {
                    println("版本 $version 的补丁文件超过了2Gb大小的限制，请将文件分多次更新以绕过此限制")
                    return false
                }

                tempPatchFile.copy(patchFile)
                tempPatchFile.delete()
            }

            versionMeta.oldFiles.addAll(diff.oldFiles)
            versionMeta.oldFolders.addAll(diff.oldFolders)
            versionMeta.newFolders.addAll(diff.newFolders)

            versionMeta.patchHash = if (patchFile.exists) HashUtils.sha1(patchFile.file) else ""
            versionMeta.patchLength = if (patchFile.exists) patchFile.length else 0
        }

        versionMeta.changeLogs = editor.get() ?: ""
        metaFile.content = versionMeta.serializeToJson().toString(4)

        val isValid = !patchFile.exists || validate(version, metaFile, patchFile)

        if (isValid)
        {
            println("正在同步history目录，可能需要一点时间")
            history.applyDiff(diff, workspaceDir)
            val elapse = System.currentTimeMillis() - start
            println("创建版本 $version 完成，耗时 ${elapse.toFloat() / 1000} 秒")
        }

        return isValid
    }

    /**
     * 校验
     */
    private fun validate(version: String, metaFile: File2, patchFile: File2): Boolean
    {
        println("正在验证 $version 的补丁文件")

        val meta = VersionMetadata(JSONObject(metaFile.content))

        if (patchFile.length != meta.patchLength || HashUtils.sha1(patchFile.file) != meta.patchHash)
        {
            println("版本 $version 的补丁文件校验失败，文件大小不相等或者哈希不匹配")
            return false
        }

        patchFile.file.bufferedInputStream().use { patch ->
            var pointer = 0L

            ByteArrayOutputStream().use { temp ->

                for ((index, newFile) in meta.newFiles.withIndex())
                {
                    temp.reset()
                    println("验证文件(${index + 1}/${meta.newFiles.size}): ${newFile.path}")

                    if (newFile.mode == ModificationMode.Modify || newFile.mode == ModificationMode.Fill)
                    {
                        patch.actuallySkip(newFile.blockOffset - pointer)

                        // 检查压缩后的二进制块
                        patch.copyAmountTo(temp, 128 * 1024, newFile.blockLength)
                        val compressedBlock = temp.toByteArray()
                        if (HashUtils.sha1(compressedBlock) != newFile.blockHash)
                        {
                            println("补丁文件中的 ${newFile.path} 文件的二进制数据部分无法通过验证，可能是文件损坏")
                            return false
                        }

                        // 检查解压后的二进制块
                        ByteArrayInputStream(compressedBlock).use { temp2 ->
                            ByteArrayOutputStream().use { decompressed ->
                                val bzip = CBZip2InputStream(temp2)
                                bzip.copyAmountTo(decompressed, 128 * 1024, newFile.rawLength)

                                val hashDecompressed = HashUtils.sha1(decompressed.toByteArray())

                                var pass = true
                                pass = pass and (newFile.mode != ModificationMode.Modify || hashDecompressed == newFile.patchFileHash)
                                pass = pass and (newFile.mode != ModificationMode.Fill || hashDecompressed == newFile.newFileHash)

                                if (!pass)
                                {
                                    println("补丁文件中的 ${newFile.path} 文件的二进制数据解压后无法通过验证，可能是文件损坏")
                                    return false
                                }
                            }
                        }

                        pointer = newFile.blockOffset + newFile.blockLength
                    }
                }
            }
        }

        println("补丁文件验证完成")
        return true
    }

    fun loop()
    {
        versionList.reload()
        println("输入你要创建的版本号名称... ${versionList.getNewest3()}")
        val newVersion = mcpatch.core.Input.readAnyString().trim()

        val metaFile = publicDir + "$newVersion.mc-patch.json"
        val patchFile = publicDir + "$newVersion.mc-patch.bin"

        versionList.reload()

        // 检查版本重复创建
        if (newVersion in versionList)
        {
            println("版本 $newVersion 已经存在，不能重复创建")
            return
        }

        // 打开更新记录编辑器
        val changeLogsFile = workdir + "changelogs.txt"
        val editor = ExternalTextFileEditor(changeLogsFile)
        editor.open()

        // 开始创建版本
        val success = create(newVersion, metaFile, patchFile, editor)

        if (success)
        {
            editor.close()
            versionList.versions.add(newVersion)
            versionList.save()
        } else {
//            metaFile.delete()
//            patchFile.delete()
            println("创建版本 $newVersion 失败！")
        }
    }
}