package mcpatch

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

object ResponseHelper
{
    private fun cors(response: NanoHTTPD.Response): NanoHTTPD.Response
    {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "*")
        response.addHeader("Access-Control-Allow-Headers", "*")
        response.addHeader("Access-Control-Allow-Credentials", "true")

        return response
    }

    fun buildFileResponse(file: File): NanoHTTPD.Response
    {
        return try {
            cors(NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                NanoHTTPD.getMimeTypeForFile("file:///" + file.path),
                FileInputStream(file),
                file.length().toInt().toLong()
            ))
        } catch (e: IOException) {
            buildForbiddenResponse("Reading file failed.")
        }
    }

    fun buildFileStreamResponse(stream: InputStream, filename: String, length: Long): NanoHTTPD.Response
    {
        return try {
            cors(NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.OK,
                NanoHTTPD.getMimeTypeForFile("file:///$filename"),
                stream,
                length
            ))
        } catch (e: IOException) {
            buildForbiddenResponse("Reading file failed.")
        }
    }

    fun buildPlainTextResponse(text: String): NanoHTTPD.Response {
        return cors(NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            NanoHTTPD.MIME_PLAINTEXT,
            text
        ))
    }

    fun buildJsonTextResponse(jsonInText: String): NanoHTTPD.Response
    {
        return cors(NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK,
            "text/json",
            jsonInText
        ))
    }

    fun buildForbiddenResponse(s: String): NanoHTTPD.Response
    {
        return cors(NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.FORBIDDEN,
            NanoHTTPD.MIME_PLAINTEXT,
            "FORBIDDEN: $s"
        ))
    }

    fun buildInternalErrorResponse(s: String): NanoHTTPD.Response
    {
        return cors(NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.INTERNAL_ERROR,
            NanoHTTPD.MIME_PLAINTEXT,
            "INTERNAL ERROR: $s"
        ))
    }

    fun buildNotFoundResponse(path: String): NanoHTTPD.Response
    {
        return cors(NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.NOT_FOUND,
            NanoHTTPD.MIME_PLAINTEXT,
            "Error 404, file not found: $path"
        ))
    }
}