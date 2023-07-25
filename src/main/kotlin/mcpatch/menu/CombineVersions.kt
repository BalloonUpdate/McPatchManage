package mcpatch.menu

import mcpatch.McPatchManage
import mcpatch.classextension.FileExtension.bufferedOutputStream
import mcpatch.core.PatchFileReader
import mcpatch.core.VersionList
import mcpatch.editor.TextFileEditor
import mcpatch.exception.McPatchManagerException
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File

class CombineVersions : BaseInteractive() {
    override fun run() {
        log.info("即将进行版本合并操作，请输入要合并的版本数量")

        val input = readInput("\\d+")

        if (!input.first)
            throw McPatchManagerException("合并过程中断")

        val count = input.second.toInt()
        val versions = McPatchManage.versionList.read()

        if (count > versions.size)
            throw McPatchManagerException("现有版本数量 ${versions.size} 小于输入的数量 $count，合并过程中断")

        val combined = versions.take(count)

        log.info("将要合并这些版本 $combined 要继续吗？（输入y或者n）")
        if (!readYesOrNot(false))
            throw McPatchManagerException("合并过程中断")

        McPatchManage.combineDir.delete()
        McPatchManage.combineDir.mkdirs()

        val workspace = File(McPatchManage.combineDir, "workspace")
        val history = File(McPatchManage.combineDir, "history")
        val public = File(McPatchManage.combineDir, "public")

        workspace.mkdirs()
        history.mkdirs()
        public.mkdirs()

        val changelogList = mutableListOf<String>()

        for (version in combined)
        {
            val patchFile = File(McPatchManage.publicDir, "$version.mcpatch.zip")

            if (!patchFile.exists())
                throw McPatchManagerException("版本 ${patchFile.path} 的数据文件丢失或者不存在，版本合并失败")

            val reader = PatchFileReader(version, ZipFile(patchFile, "utf-8"))

            changelogList.add(reader.meta.changeLogs)

            // 删除旧文件和旧目录，还有创建新目录
            reader.meta.oldFiles.map { File(workspace, it) }.forEach { it.delete() }
            reader.meta.oldFolders.map { File(workspace, it) }.forEach { it.delete() }
            reader.meta.newFolders.map { File(workspace, it) }.forEach { it.mkdirs() }

            for ((index, entry) in reader.withIndex())
            {
                log.info("[$version] 解压(${index + 1}/${reader.meta.newFiles.size}) ${entry.meta.path}")

                val file = File(workspace, entry.meta.path)

                file.parentFile.mkdirs()

                file.bufferedOutputStream().use { stream -> entry.copyTo(stream) }
            }
        }

        val versionL = VersionList(File(public, "versions.txt"))
        val changelogs = TextFileEditor(File(McPatchManage.workdir, "changelogs.txt"))

        // 生成默认更新记录
        val defaultChangeLogs = changelogList
            .filter { it.isNotEmpty() }
            .joinToString("\n\n==================================================\n\n")
            .trim()
        changelogs.write(defaultChangeLogs)

        if (changelogList.isNotEmpty())
        {
            log.info("请打开 ${changelogs.file.name} 编辑合并后的更新记录，然后按任意键继续")

            readAnyString()
        }

        log.info("请输入 $combined 版本在合并后新的版本号")

        var version: String

        while (true)
        {
            version = readAnyString()

            if (version.isEmpty())
            {
                log.info("版本号不能为空，请重新输入")
                continue
            }

            if (version in McPatchManage.versionList.read().filter { !combined.contains(it) })
            {
                log.info("版本号与已有版本号重复，请重新输入")
                continue
            }

            break
        }

        CreateVersion().create(workspace, history, public, true, versionL, changelogs, version)

        log.info("正在进行收尾工作")

        // 生成新的版本号列表
        val existings = McPatchManage.versionList.read()
        existings.removeIf { it in combined }
        val ddd = listOf(version) + existings
        log.info(ddd)
        McPatchManage.versionList.write(ddd)

        // 删除合并之前的版本文件
        for (c in combined)
            File(McPatchManage.publicDir, "$c.mcpatch.zip").delete()

        // 复制新的版本文件
        val patchFile = File(public, "$version.mcpatch.zip")
        patchFile.copyTo(File(McPatchManage.publicDir, patchFile.name))

        McPatchManage.combineDir.delete()

        log.info("版本合并完成: $combined => $version")
    }
}