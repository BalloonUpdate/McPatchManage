package mcpatch.interactive

import com.lee.bsdiff.BsDiff
import mcpatch.McPatchManage
import mcpatch.McPatchManage.historyDir
import mcpatch.McPatchManage.publicDir
import mcpatch.McPatchManage.versionList
import mcpatch.McPatchManage.workspaceDir
import mcpatch.core.Input
import mcpatch.core.PatchFileReader
import mcpatch.data.ModificationMode
import mcpatch.data.NewFile
import mcpatch.data.VersionData
import mcpatch.diff.DirectoryDiff
import mcpatch.diff.RealFile
import mcpatch.editor.TextFileEditor
import mcpatch.exception.McPatchManagerException
import mcpatch.extension.FileExtension.bufferedInputStream
import mcpatch.extension.FileExtension.bufferedOutputStream
import mcpatch.extension.RuntimeExtension.usedMemory
import mcpatch.extension.StreamExtension.copyAmountTo
import mcpatch.utils.File2
import mcpatch.utils.HashUtils
import mcpatch.utils.MiscUtils
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.tools.bzip2.CBZip2InputStream
import org.apache.tools.bzip2.CBZip2OutputStream
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

class Create
{
    private fun packFile(
        diff: DirectoryDiff,
        versionMeta: VersionData,
        sharedBuf: ByteArrayOutputStream,
        output: ZipArchiveOutputStream,
        index: Int,
        newFile: String
    ) {
        val old = historyDir + newFile
        val new = workspaceDir + newFile
        val newLen = if (new.exists) new.length else 0
        val oldLen = if (old.exists) old.length else 0
        val case = old.name != new.name && old.name.equals(new.name, ignoreCase = true)
        val mode = when {
//            newFile in overwroteFiles -> ModificationMode.Fill
            newLen == 0L -> ModificationMode.Empty
            (oldLen == 0L && newLen > 0) || case -> ModificationMode.Fill
            else -> ModificationMode.Modify
        }

        if (max(oldLen, newLen) > Int.MAX_VALUE.toLong() - 1)
            throw McPatchManagerException("暂时不支持打包大小超过2GB的文件： $newFile")

        println("打包文件(${index + 1}/${diff.newFiles.size}) $newFile")

        sharedBuf.reset()

        when (mode)
        {
            ModificationMode.Fill -> {
                new.file.bufferedInputStream().use { stream ->
                    // 压缩
                    val bzip = CBZip2OutputStream(sharedBuf)

                    val rawLength: Long
                    val rawHash: String
                    ByteArrayOutputStream().use { temp ->
                        rawLength = stream.copyAmountTo(temp, 128 * 1024, stream.available().toLong())
                        rawHash = HashUtils.sha1(temp.toByteArray())
                        temp.writeTo(bzip)
                    }
                    bzip.finish()
                    bzip.flush()

                    // 写出数据
                    val entry = ZipArchiveEntry(newFile)
                    entry.size = sharedBuf.size().toLong()
                    output.putArchiveEntry(entry)
                    sharedBuf.writeTo(output)
                    output.closeArchiveEntry()

                    versionMeta.newFiles += NewFile(
                        path = newFile,
                        mode = mode,
                        oldHash = "",
                        newHash = HashUtils.sha1(new.file),
                        bzippedHash = HashUtils.sha1(sharedBuf.toByteArray()),
                        rawHash = rawHash,
                        rawLength = rawLength
                    )
                }
            }

            ModificationMode.Empty -> {
                versionMeta.newFiles += NewFile(newFile, mode, "", "", "", "", 0)
            }

            ModificationMode.Modify -> { // 计算差异
                new.file.bufferedInputStream().use { n ->
                    old.file.bufferedInputStream().use { o ->
                        // 压缩
                        val bzip = CBZip2OutputStream(sharedBuf)

                        val rawLength: Long
                        val rawHash: String
                        ByteArrayOutputStream().use { temp ->
                            rawLength = BsDiff().bsdiff(o, n, temp, o.available(), n.available())
                            rawHash = HashUtils.sha1(temp.toByteArray())
                            temp.writeTo(bzip)
                        }
                        bzip.finish()
                        bzip.flush()

                        // 写出数据
                        val entry = ZipArchiveEntry(newFile)
                        entry.size = sharedBuf.size().toLong()
                        output.putArchiveEntry(entry)
                        sharedBuf.writeTo(output)
                        output.closeArchiveEntry()

                        versionMeta.newFiles += NewFile(
                            path = newFile,
                            mode = mode,
                            oldHash = HashUtils.sha1(old.file),
                            newHash = HashUtils.sha1(new.file),
                            bzippedHash = HashUtils.sha1(sharedBuf.toByteArray()),
                            rawHash = rawHash,
                            rawLength = rawLength
                        )
                    }
                }
            }
        }
    }

    /**
     * 创建版本
     * @param version 新版本名称
     * @param outputFile 输出的临时文件
     * @param diff 文件差异
     * @param changelogs 更新记录编辑器
     */
    private fun create(version: String, outputFile: File2, diff: DirectoryDiff, changelogs: TextFileEditor)
    {
        println("正在创建版本 $version 可能需要一点时间")

        // 开始打包
        outputFile.file.bufferedOutputStream(8 * 1024 * 1024).use { tempFile2 ->
            val archive = ZipArchiveOutputStream(tempFile2)
            archive.encoding = "utf-8"
            val versionMeta = VersionData()

            // 写出文件更新数据
            if (diff.newFiles.isNotEmpty())
                ByteArrayOutputStream().use { sharedBuf ->
                    for ((index, newFile) in diff.newFiles.withIndex())
                        packFile(diff, versionMeta, sharedBuf, archive, index, newFile)
                }

            if (outputFile.length > Int.MAX_VALUE)
                throw McPatchManagerException("版本 $version 的补丁文件超过了2Gb大小的限制，请将文件分多次更新以绕过此限制")

            versionMeta.oldFiles.addAll(diff.oldFiles)
            versionMeta.oldFolders.addAll(diff.oldFolders)
            versionMeta.newFolders.addAll(diff.newFolders)
            versionMeta.changeLogs = changelogs.get() ?: ""

            // 写出元数据
            val bytes = versionMeta.serializeToJson().toString(4).encodeToByteArray()
            val entry = ZipArchiveEntry(".mcpatch-meta.json")
            entry.size = bytes.size.toLong()
            archive.putArchiveEntry(entry)
            archive.write(bytes)
            archive.closeArchiveEntry()

            archive.finish()
        }
    }

    fun loop()
    {
        println("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(workspaceDir)
        val history = RealFile.CreateFromRealFile(historyDir)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(workspace.files, history.files)

        if (hasDiff)
        {
            println("----------以下为文件修改列表（共 ${diff.totalDiff} 处文件改动）----------")
            println(diff.toString("旧目录", "新目录", "旧文件", "新文件"))
            println("----------以上为文件修改列表（共 ${diff.totalDiff} 处文件改动）----------")
        } else {
            println("没有任何文件改动，即将创建一个空版本")
        }

        // 提示输入版本号
        println("输入你要创建的版本号名称，目前最新版本号为 ${versionList.getNewest()}")
        val version = Input.readAnyString().trim()

        if (version.isEmpty())
            throw McPatchManagerException("版本号不能为空，创建过程中断")

        // 检查版本重复创建
        if (versionList.read().contains(version))
            throw McPatchManagerException("版本 $version 已经存在，不能重复创建，创建过程中断")

        // 准备创建版本
        val patchFile = publicDir + "$version.mcpatch.zip"
        val tempPatchFile = patchFile.parent + (patchFile.name + ".temporal.bin")

        val changelogs = TextFileEditor(McPatchManage.workdir + "changelogs.txt")
        changelogs.open()

        // 计算预计内存消耗
        val totalMemory = (diff.newFiles.sumOf {
            val old = historyDir + it
            val new = workspaceDir + it
            val newLen = if (new.exists) new.length else 0
            val oldLen = if (old.exists) old.length else 0

            newLen + oldLen + (oldLen * 8)
        } * 1.3f).toLong() + (Runtime.getRuntime().usedMemory() * 1.2f).toLong()

        // 给出提示信息
        val ramRequired = MiscUtils.convertBytes(totalMemory)
        val ramAvaliable = MiscUtils.convertBytes(Runtime.getRuntime().maxMemory())
        println("打包过程预计消耗内存: $ramRequired, JVM总可用内存为: $ramAvaliable")

        if (totalMemory > Runtime.getRuntime().maxMemory())
            println("\n检测到可用内存不足，打包过程可能发生崩溃，请使用JVM参数Xmx调整最大可用内存！\n")

        println("如果有更新记录请在此时粘贴到 changelogs.txt 文件里")
        println("确定要创建版本 $version 吗? （输入y或者n）")

        if (!Input.readYesOrNot(false))
            throw McPatchManagerException("创建过程中断")

        // 创建更新包
        val start = System.currentTimeMillis()
        create(version, tempPatchFile, diff, changelogs)

        println("正在验证 $version 的补丁文件")

        // 校验更新包
        try {
            PatchFileReader(version, tempPatchFile).use { reader ->
                val buf = ByteArrayOutputStream()
                for ((index, entry) in reader.withIndex())
                {
                    println("验证文件(${index + 1}/${reader.meta.newFiles.size}): ${entry.newFile.path}")
                    buf.reset()
                    entry.read(buf)
                }
            }
        } catch (e: McPatchManagerException) {
            println(e)
            throw McPatchManagerException("版本 $version 的补丁文件校验失败")
        }

        println("补丁文件验证完成")

        // 同步history
        println("正在同步文件状态，可能需要一点时间")
        history.applyDiff(diff, workspaceDir)

        // 合并临时文件
        tempPatchFile.copy(patchFile)
        tempPatchFile.delete()

        // 删除更新记录文件
        changelogs.close()

        // 更新版本号
        val versions = versionList.read()
        versions.add(version)
        versionList.write(versions)

        // 输出耗时
        val elapse = System.currentTimeMillis() - start
        println("创建版本 $version 完成，耗时 ${elapse.toFloat() / 1000} 秒")
    }
}