package org.tdf.sunflower.controller

import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.tdf.common.util.ByteUtil
import org.tdf.sunflower.util.MapperUtil
import java.io.*
import java.nio.charset.Charset
import java.util.*
import java.util.function.Consumer
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

@Component
class JsonRpcFilter : Filter {
    val mapper = MapperUtil.OBJECT_MAPPER


    override fun init(filterConfig: FilterConfig) {
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (!(request is HttpServletRequest && response is HttpServletResponse)) {
            throw RuntimeException("JsonRpcUsageFilter supports only HTTP requests.")
        }

        // don't count alias as it redirects here
        val isJsonRpcUrl = "/" == request.requestURI
        if (isJsonRpcUrl && request.method.equals("POST", ignoreCase = true)) {
            try {
                val wrappedRequest = ResettableStreamHttpServletRequest(
                    request
                )
                val body = IOUtils.toString(wrappedRequest.reader)
                wrappedRequest.resetInputStream()
                if (response.getCharacterEncoding() == null) {
                    response.setCharacterEncoding("UTF-8")
                }
                val responseCopier = HttpServletResponseCopier(response)
                try {
                    chain.doFilter(wrappedRequest, responseCopier)
                    responseCopier.flushBuffer()
                } finally {
                    // read response for stats and log
                    val copy = responseCopier.copy
                    val responseText = String(copy, Charset.forName(response.characterEncoding))
                    val json = mapper.readTree(body)
                    val responseJson = mapper.readTree(responseText)
                    if (json.isArray) {
                        for (i in 0 until json.size()) {
                            notifyInvocation(json[i], responseJson[i])
                        }
                    } else {
                        notifyInvocation(json, responseJson)
                    }

                    // According to spec, JSON-RPC 2 should return status 200 in case of error
                    if (response.status == 500) {
                        response.status = 200
                    }
                }
            } catch (e: IOException) {
                log.error("Error parsing JSON-RPC request", e);
            }
        } else {
            chain.doFilter(request, response)
        }
    }

    private fun notifyInvocation(requestJson: JsonNode, responseJson: JsonNode) {
        if (responseJson.has("error")) {
            val errorMessage = responseJson["error"].toString()
            log.error("Problem when invoking JSON-RPC $requestJson response:$errorMessage")
        } else {
            val methodName = requestJson["method"].asText()
            val params: MutableList<JsonNode> = ArrayList()
            if (requestJson.has("params")) {
                requestJson["params"]
                    .forEach(Consumer { e: JsonNode -> params.add(e) })
            }
            val responseText = mapper.writeValueAsString(responseJson)
            if (log.isInfoEnabled) {
                // passwords could be sent here
                if (!EXCLUDE_LOGS.contains(methodName)) {
                    log.info("rpc method = {}", methodName)
                } else {
                    // logging is handled manually in service
                }
            }
        }
    }

    private fun dumpToString(n: JsonNode): String? {
        return if (n.isTextual) {
            n.asText()
        } else {
            try {
                return mapper.writeValueAsString(n)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            null
        }
    }

    override fun destroy() {}

    private class ResettableStreamHttpServletRequest(private val request: HttpServletRequest) :
        HttpServletRequestWrapper(
            request
        ) {
        private var rawData: ByteArray? = null

        private var servletStream: ResettableServletInputStream

        fun resetInputStream() {
            servletStream.stream = ByteArrayInputStream(rawData)
        }

        override fun getInputStream(): ServletInputStream {
            if (rawData == null) {
                rawData = IOUtils.toByteArray(this.request.reader)
                servletStream.stream = ByteArrayInputStream(rawData)
            }
            return servletStream
        }

        override fun getReader(): BufferedReader {
            if (rawData == null) {
                rawData = IOUtils.toByteArray(this.request.reader)
            }
            servletStream = ResettableServletInputStream()
            servletStream.stream = ByteArrayInputStream(rawData)
            return BufferedReader(InputStreamReader(servletStream))
        }

        private class ResettableServletInputStream : ServletInputStream() {
            var stream: InputStream? = null

            override fun read(): Int {
                return stream!!.read()
            }

            override fun isFinished(): Boolean {
                return false
            }

            override fun isReady(): Boolean {
                return true
            }

            override fun setReadListener(listener: ReadListener) {}
        }

        init {
            servletStream = ResettableServletInputStream()
        }
    }

    class ServletOutputStreamCopier(private val outputStream: OutputStream) : ServletOutputStream() {
        private val copy: ByteArrayOutputStream = ByteArrayOutputStream(1024)

        override fun write(b: Int) {
            outputStream.write(b)
            copy.write(b)
        }

        fun getCopy(): ByteArray {
            return copy.toByteArray()
        }

        override fun isReady(): Boolean {
            return true
        }

        override fun setWriteListener(listener: WriteListener) {}

    }

    class HttpServletResponseCopier(response: HttpServletResponse) : HttpServletResponseWrapper(response) {
        private var outputStream: ServletOutputStream? = null
        private var writer: PrintWriter? = null
        private var copier: ServletOutputStreamCopier? = null

        override fun getOutputStream(): ServletOutputStream {
            check(writer == null) { "getWriter() has already been called on this response." }
            if (outputStream == null) {
                outputStream = response.outputStream
                copier = ServletOutputStreamCopier(response.outputStream)
            }
            return copier!!
        }

        override fun getWriter(): PrintWriter {
            check(outputStream == null) { "getOutputStream() has already been called on this response." }
            if (writer == null) {
                copier = ServletOutputStreamCopier(response.outputStream)
                writer = PrintWriter(OutputStreamWriter(copier!!, response.characterEncoding), true)
            }
            return writer!!
        }

        override fun flushBuffer() {
            if (writer != null) {
                writer!!.flush()
            } else if (outputStream != null) {
                copier!!.flush()
            }
        }

        val copy: ByteArray
            get() = copier?.getCopy() ?: ByteUtil.EMPTY_BYTE_ARRAY
    }

    companion object {
        private val EXCLUDE_LOGS = listOf(
            "eth_getLogs", "eth_getFilterLogs",
            "personal_newAccount", "personal_importRawKey", "personal_unlockAccount", "personal_signAndSendTransaction"
        )
        private val log = LoggerFactory.getLogger("jsonrpc")
    }
}