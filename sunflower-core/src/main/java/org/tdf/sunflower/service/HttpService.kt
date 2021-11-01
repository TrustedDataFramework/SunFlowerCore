package org.tdf.sunflower.service

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets

@Service
class HttpService {
    private val restTemplate = RestTemplate(listOf(StringHttpMessageConverter(StandardCharsets.UTF_8)))

    /**
     * 发送 get 请求
     *
     * @param httpHeaders 头部
     * @param uri         uri
     * @param parameters  问号后面跟的参数
     * @return 响应体
     */
    fun get(
        uri: String,
        httpHeaders: HttpHeaders = HttpHeaders.EMPTY,
        parameters: Map<String, String> = emptyMap()
    ): String {
        return request(HttpMethod.GET, httpHeaders, uri, parameters, "")
    }

    /**
     * 发起一次 http 请求
     *
     * @param method      请求方法 get/post/put/patch/delete
     * @param httpHeaders 头部
     * @param uri         uri
     * @param query       问号后面跟的参数
     * @param body        请求体
     * @return 响应体
     */
    fun request(
        method: HttpMethod,
        httpHeaders: HttpHeaders,
        uri: String,
        query: Map<String, String>,
        body: String
    ): String {
        val req = RequestEntity
            .method(method, buildURI(uri, query))
            .headers(httpHeaders)
            .body(body)
        return restTemplate
            .exchange(req, String::class.java)
            .body
    }

    private fun buildURI(uri: String, query: Map<String, String>): URI {
        val multiMap: MutableMap<String, List<String>> = HashMap()
        query.forEach { (x: String, y: String) -> multiMap[x] = listOf(y) }
        val builder = UriComponentsBuilder
            .newInstance()
            .uri(URI.create(uri))
            .queryParams(LinkedMultiValueMap(multiMap))
        return builder.build().toUri()
    }
}