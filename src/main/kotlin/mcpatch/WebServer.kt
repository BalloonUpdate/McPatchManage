package mcpatch

import fi.iki.elonen.NanoWSD
import mcpatch.classextension.FileExtension.bufferedOutputStream
import mcpatch.classextension.StreamExtension.copyAmountTo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.text.SimpleDateFormat

class WebServer(host: String, port: Int) : NanoWSD(host, port) {
    private val fmt = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")
//    private val jarFile = if (webDir == null) JarFile(getJarFile().path) else null

    override fun openWebSocket(handshake: IHTTPSession): WebSocket {
        return WebsocketServe(handshake)
    }

    override fun serveHttp(session: IHTTPSession): Response {
        val timestamp = fmt.format(System.currentTimeMillis())
        val start = System.currentTimeMillis()
        val res: Response = processRequest(session)
        val elapsed = System.currentTimeMillis() - start
        val statusCode = res.status.requestStatus
        val uri = session.uri
        val ip: String = session.javaClass.getDeclaredField("remoteIp").also { it.isAccessible = true }.get(session) as String

        println(String.format("[ %s ] %3s | %-15s | %s (%dms)", timestamp, statusCode, ip, uri, elapsed))

        return res
    }

    private fun processRequest(session: IHTTPSession): Response {
        try {
            val contentLength = session.headers["content-length"]?.toLong()

            val parsed = URI("http://localhost" + session.uri).toURL()
            val path = parsed.path.replace('\\', '/')

            // Prohibit getting out of current directory
            if ("../" in path)
                return ResponseHelper.buildForbiddenResponse("Won't serve ../ for security reasons.")

            val (first, rest) = separateFirst(path.split('/').filter { it.isNotEmpty() })

            return when (first) {
                "explorer" -> explorer(rest, parsed, contentLength, session)
                else -> ResponseHelper.buildForbiddenResponse("unknown action '$first'")
            }

        } catch (e: Exception) {
            return ResponseHelper.buildInternalErrorResponse(e.stackTraceToString())
        }
    }

    private fun explorer(arguments: List<String>, url: URL, contentLength: Long?, session: IHTTPSession): Response {
        val (action, paths) = separateFirst(arguments)
        val path = paths.joinToString("/")

        val file = if (path.isNotEmpty()) File(McPatchManage.workspaceDir, path) else McPatchManage.workspaceDir

        return when (action) {
            "list" -> {
                if (!file.exists())
                    return ResponseHelper.buildForbiddenResponse("the file does not exist: '$path'")

                if (!file.isDirectory)
                    return ResponseHelper.buildForbiddenResponse("the file is not a directory: '$path'")

                val output = JSONArray()

                for (f in file.listFiles()!!) {
                    val o = JSONObject()
                    o.put("name", f.name)
                    o.put("modified", if (f.isFile) f.lastModified() / 1000 else 0)
                    o.put("length", if (f.isFile) f.length() else -1)
                    output.put(o)
                }

                ResponseHelper.buildPlainTextResponse(output.toString(4))
            }
            "read" -> {
                if (!file.exists())
                    return ResponseHelper.buildForbiddenResponse("the file does not exist: '$path'")

                if (file.isFile)
                    return ResponseHelper.buildForbiddenResponse("the file is not a file: '$path'")

                ResponseHelper.buildFileResponse(file)
            }
            "write" -> {
                if (session.method != Method.POST)
                    return ResponseHelper.buildForbiddenResponse("wrong http method: '$path'")

                val cl = contentLength ?: return ResponseHelper.buildForbiddenResponse("content-length is missing from the request header")

                if (file.exists() && file.isDirectory)
                    return ResponseHelper.buildForbiddenResponse("the file is a directory: '$path'")

                file.bufferedOutputStream().use { session.inputStream.copyAmountTo(it, cl) }

                ResponseHelper.buildPlainTextResponse("success")
            }
            "delete" -> {
                if (!file.exists())
                    return ResponseHelper.buildForbiddenResponse("the file does not exist: '$path'")

                if (file.isDirectory)
                    file.deleteRecursively()
                else
                    file.delete()

                ResponseHelper.buildPlainTextResponse("success")
            }
            "move" -> {
                ResponseHelper.buildPlainTextResponse("success")
            }
            else -> ResponseHelper.buildForbiddenResponse("unknown explorer action '$action'")
        }
    }

//    private fun terminal(arguments: List<String>, url: URL, contentLength: Long, session: IHTTPSession): Response {
//
//    }

    private fun separateFirst(strings: List<String>): Pair<String, List<String>> {
        return strings.run { Pair(
            if (this.isNotEmpty()) this[0] else "",
            if (this.size > 1) this.slice(1 until this.size) else listOf()
        ) }
    }
}