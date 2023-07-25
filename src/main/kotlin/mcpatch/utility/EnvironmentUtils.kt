package mcpatch.utility

import java.io.IOException
import java.net.URLDecoder
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

object EnvironmentUtils
{
    /**
     * 程序是否被打包
     */
    @get:JvmName("isPackaged")
    @JvmStatic
    val isPackaged: Boolean get() = javaClass.getResource("").protocol != "file"

    /**
     * 获取当前Jar文件路径（仅打包后有效）
     */
    @JvmStatic
    val jarFile: File2
        get() {
        val url = URLDecoder.decode(EnvironmentUtils.javaClass.protectionDomain.codeSource.location.file, "UTF-8").replace("\\", "/")
        return File2(if (url.endsWith(".class") && "!" in url) {
            val path = url.substring(0, url.lastIndexOf("!"))
            if ("file:/" in path) path.substring(path.indexOf("file:/") + "file:/".length) else path
        } else url)
    }

    val version: String by lazy { manifest["Version"] ?: "0.0.0" }
    val gitCommit: String by lazy { manifest["Git-Commit"] ?: "<development>" }

    /**
     * 读取版本信息（程序打包成Jar后才有效）
     * @return Application版本号，如果为打包成Jar则返回null
     */
    val manifest: Map<String, String> get() {
        return try {
            originManifest.entries.associate { it.key.toString() to it.value.toString() }
        } catch (e: IOException) {
            mapOf()
        }
    }

    val originManifest: Attributes
        get() {
            if(!isPackaged)
                throw IOException("Manifest信息获取失败")

            JarFile(jarFile.path).use { jar ->
                jar.getInputStream(jar.getJarEntry("META-INF/MANIFEST.MF")).use {
                    return Manifest(it).mainAttributes
                }
            }
        }
}