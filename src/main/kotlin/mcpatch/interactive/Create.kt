package mcpatch.interactive

import com.lee.bsdiff.BsDiff
import mcpatch.McPatchManage
import mcpatch.core.Input
import mcpatch.core.PatchFileReader
import mcpatch.core.VersionList
import mcpatch.data.ModificationMode
import mcpatch.data.MoveFile
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
import mcpatch.logging.Log
import mcpatch.stream.MemoryOutputStream
import mcpatch.utils.File2
import mcpatch.utils.HashUtils
import mcpatch.utils.MiscUtils
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.tools.bzip2.CBZip2OutputStream
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.max

class Create
{
    private fun packFile(
        old: File2,
        new: File2,
        sharedBuf: MemoryOutputStream,
        output: ZipArchiveOutputStream,
        path: String,
        overwrite: Boolean,
    ): NewFile {
        val newLen = if (new.exists) new.length else 0
        val oldLen = if (old.exists) old.length else 0
        val case = old.name != new.name && old.name.equals(new.name, ignoreCase = true)
        var mode = when {
            newLen == 0L -> ModificationMode.Empty
            (oldLen == 0L && newLen > 0) || case -> ModificationMode.Fill
            else -> ModificationMode.Modify
        }

        // 处理文件覆盖
        if (overwrite && mode == ModificationMode.Modify)
            mode = ModificationMode.Fill

        when (mode)
        {
            ModificationMode.Fill -> {
                new.file.bufferedInputStream().use { stream ->
                    // 压缩
                    val bzip = CBZip2OutputStream(sharedBuf)

                    val rawLength: Long
                    val rawHash: String
                    ByteArrayOutputStream().use { temp ->
                        rawLength = stream.copyAmountTo(temp, stream.available().toLong())
                        rawHash = HashUtils.sha1(temp.toByteArray())
                        temp.writeTo(bzip)
                    }
                    bzip.finish()
                    bzip.flush()

                    // 写出数据
                    val entry = ZipArchiveEntry(path)
                    entry.size = sharedBuf.size().toLong()
                    output.putArchiveEntry(entry)
                    sharedBuf.writeTo(output)
                    output.closeArchiveEntry()

                    return NewFile(
                        path = path,
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
                return NewFile(path, mode, "", "", "", "", 0)
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
                        val entry = ZipArchiveEntry(path)
                        entry.size = sharedBuf.size().toLong()
                        output.putArchiveEntry(entry)
                        sharedBuf.writeTo(output)
                        output.closeArchiveEntry()

                        return NewFile(
                            path = path,
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

    fun create(
        workspaceD: File2,
        historyD: File2,
        outputD: File2,
        skipHistorySync: Boolean,
        versionL: VersionList,
        changelogs: TextFileEditor,
        versionSpecified: String? = null,
        extraDeleteds: List<String>? = null,
    ) {
        Log.info("正在计算文件修改，可能需要一点时间")

        val workspace = RealFile.CreateFromRealFile(workspaceD)
        val history = RealFile.CreateFromRealFile(historyD)
        val diff = DirectoryDiff()
        val hasDiff = diff.compare(history.files, workspace.files, McPatchManage.ignorefile, true)

        if (hasDiff)
        {
            Log.info("----------以下为文件修改列表（共 ${diff.totalDiff} 处文件改动）----------")
            Log.info(diff.toString(McPatchManage.overwritefile))
            Log.info("----------以上为文件修改列表（共 ${diff.totalDiff} 处文件改动）----------")
        } else {
            Log.info("没有任何文件改动，即将创建一个空版本")
        }

        // 检测仅修改大小写的问题
        val mfs = diff.missingFiles.map { it.lowercase(Locale.getDefault()) }.toList()
        val nfs = diff.redundantFiles .map { it.lowercase(Locale.getDefault()) }.toList()
        val collided = mfs.firstOrNull { it in nfs }

        if (collided != null)
            throw McPatchManagerException("无法打包仅修改大小写文件名的文件：$collided")

        val ff1 = diff.moveFiles.map { it.first.lowercase(Locale.getDefault()) }.toList()
        val tf1 = diff.moveFiles.map { it.second.lowercase(Locale.getDefault()) }.toList()
        val collided1 = ff1.firstOrNull { it in tf1 }
        if (collided1 != null)
            throw McPatchManagerException("无法打包仅修改大小写文件名的文件：$collided1")


        // 提示输入版本号
        Log.info("输入你要创建的版本号名称，目前最新版本号为 ${versionL.getNewest()}")
        val version = versionSpecified ?: Input.readAnyString().trim()

        if (version.isEmpty())
            throw McPatchManagerException("版本号不能为空，创建过程中断")

        val versions = versionL.read()

        // 检查版本重复创建
        if (versions.contains(version))
            throw McPatchManagerException("版本 $version 已经存在，不能重复创建，创建过程中断")

        val isFull = versions.isEmpty()
        if (isFull)
            Log.info("提示：版本 $version 是第一个版本，会被打包成全量更新包，后续的更新包会以增量形式进行打包")

        // 准备创建版本
        val patchFile = outputD + "$version.mcpatch.zip"
        val tempPatchFile = patchFile.parent + (patchFile.name + ".temporal.zip")

        changelogs.create()

        // 计算预计内存消耗
        val totalMemory = (diff.missingFiles.sumOf {
            val old = historyD + it
            val new = workspaceD + it
            val newLen = if (new.exists) new.length else 0
            val oldLen = if (old.exists) old.length else 0

            newLen + oldLen + (oldLen * 8)
        } * 1.3f).toLong() + (Runtime.getRuntime().usedMemory() * 1.2f).toLong()

        // 给出提示信息
        val ramRequired = MiscUtils.convertBytes(totalMemory)
        val ramAvaliable = MiscUtils.convertBytes(Runtime.getRuntime().maxMemory())
        Log.info("打包过程预计消耗内存: $ramRequired, JVM总可用内存为: $ramAvaliable")

        if (totalMemory > Runtime.getRuntime().maxMemory())
            Log.info("\n检测到可用内存不足，打包过程可能发生崩溃，请使用JVM参数Xmx调整最大可用内存！\n")

        Log.info("如果有更新记录请在此时粘贴到 changelogs.txt 文件里")
        Log.info("确定要创建版本 $version 吗? （输入y或者n）")

        var overwritesAll = false

        if (versionSpecified == null)
        {
            if (!Input.readYesOrNot(false))
                throw McPatchManagerException("创建过程中断")

            Log.info("要对这个版本中的所有变动文件使用强制覆盖吗? 否则沿用 overwrites.txt 文件规则，不知道此选项的功能请选择n（输入y或者n）")
            overwritesAll = Input.readYesOrNot(false)
        }

        // 创建更新包
        val start = System.currentTimeMillis()
        Log.info("正在创建版本 $version 可能需要一点时间")

        try {


            tempPatchFile.file.bufferedOutputStream(8 * 1024 * 1024).use { tempFile2 ->
                val archive = ZipArchiveOutputStream(tempFile2)
                archive.encoding = "utf-8"

                val meta = VersionData()

                meta.moveFiles.addAll(diff.moveFiles.map { MoveFile(it.first, it.second) })
                meta.oldFiles.addAll(diff.redundantFiles)
                if (extraDeleteds != null)
                    meta.oldFiles.addAll(extraDeleteds)
                meta.oldFolders.addAll(diff.redundantFolders)
                meta.newFolders.addAll(diff.missingFolders)
                meta.changeLogs = changelogs.get() ?: ""

                // 写出文件更新数据
                if (diff.missingFiles.isNotEmpty())
                {
                    MemoryOutputStream().use { sharedBuf ->
                        val overwrites = McPatchManage.overwritefile
                        overwrites.reload()

                        for ((index, path) in diff.missingFiles.withIndex())
                        {
                            sharedBuf.reset()

                            val old = historyD + path
                            val new = workspaceD + path
                            val newLen = if (new.exists) new.length else 0
                            val oldLen = if (old.exists) old.length else 0
                            val overwrite = overwritesAll || path in overwrites

                            Log.info("打包文件(${index + 1}/${diff.missingFiles.size}) $path")

                            if (max(newLen, oldLen) > Int.MAX_VALUE.toLong() - 1)
                                throw McPatchManagerException("暂时不支持打包大小超过2GB的文件： $path")

                            meta.newFiles.add(packFile(old, new, sharedBuf, archive, path, overwrite))
                        }
                    }
                }

                // 全量包在安装之前会删除所有跟踪的文件，已达到强制更新的目的
                if (isFull)
                    meta.oldFiles.addAll(diff.missingFiles)

                // 写出元数据
                val bytes = meta.serializeToJson().toString(4).encodeToByteArray()
                val entry = ZipArchiveEntry(".mcpatch-meta.json")
                entry.size = bytes.size.toLong()
                archive.putArchiveEntry(entry)
                archive.write(bytes)
                archive.closeArchiveEntry()

                archive.finish()
            }

            if (tempPatchFile.length > Int.MAX_VALUE)
                throw McPatchManagerException("版本 $version 的补丁文件超过了2Gb大小的限制，请将文件分多次更新以绕过此限制")

            Log.info("正在验证 $version 的补丁文件")

            // 校验更新包
            try {
                PatchFileReader(version, ZipFile(tempPatchFile.file, "utf-8")).use { reader ->
                    val buf = ByteArrayOutputStream()
                    for ((index, entry) in reader.withIndex())
                    {
                        Log.info("验证文件(${index + 1}/${reader.meta.newFiles.size}): ${entry.meta.path}")
                        buf.reset()
                        entry.copyTo(buf)
                    }
                }
            } catch (e: McPatchManagerException) {
                Log.info(e.message!!)
                throw McPatchManagerException("版本 $version 的补丁文件校验失败")
            }

            Log.info("补丁文件验证完成")

            if (!skipHistorySync)
            {
                // 同步history
                Log.info("正在同步文件状态，可能需要一点时间")
                history.syncFrom(diff, workspaceD)
            } else {
                Log.info("已跳过文件状态同步")
            }

            // 合并临时文件
            tempPatchFile.copy(patchFile)
            tempPatchFile.delete()

            // 删除更新记录文件
            changelogs.clear()

            // 更新版本号
            val versions2 = versionL.read()
            versions2.add(version)
            versionL.write(versions2)

            // 输出耗时
            val elapse = System.currentTimeMillis() - start
            Log.info("创建版本 $version 完成，耗时 ${elapse.toFloat() / 1000} 秒")
        } catch (e: OutOfMemoryError) {
            Log.error("打包时发生错误，因为分配的内存不足导致打包失败。请尝试使用JVM参数 -Xmx8G 来给管理端分配更多可用内存")
        }
    }

    fun execute()
    {
        val changelogs = TextFileEditor(McPatchManage.workdir + "changelogs.txt")

        create(
            McPatchManage.workspaceDir,
            McPatchManage.historyDir,
            McPatchManage.publicDir,
            false,
            McPatchManage.versionList,
            changelogs,
            null
        )
    }
}