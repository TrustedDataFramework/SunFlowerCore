package org.tdf.sunflower.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HttpService {
    private RestTemplate restTemplate = new RestTemplate(Collections.singletonList(
            new StringHttpMessageConverter(StandardCharsets.UTF_8)
    ));


    /**
     * 发送 get 请求
     *
     * @param httpHeaders 头部
     * @param uri         uri
     * @param parameters  问号后面跟的参数
     * @return 响应体
     */
    public String get(
            HttpHeaders httpHeaders,
            @NonNull String uri,
            Map<String, String> parameters) {
        return request(HttpMethod.GET, httpHeaders, uri, parameters, "");
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
    public String request(
            HttpMethod method,
            HttpHeaders httpHeaders,
            String uri,
            Map<String, String> query,
            String body
    ) {
        RequestEntity<String> req = RequestEntity
                .method(method, buildURI(uri, query))
                .headers(httpHeaders)
                .body(body);

        return restTemplate
                .exchange(req, String.class)
                .getBody();
    }

    public URI buildURI(String uri, Map<String, String> query) {
        Map<String, List<String>> multiMap = new HashMap<>();
        query.forEach((x, y) -> multiMap.put(x, Collections.singletonList(y)));

        UriComponentsBuilder builder = UriComponentsBuilder
                .newInstance()
                .uri(URI.create(uri))
                .queryParams(new LinkedMultiValueMap<>(multiMap));
        return builder.build().toUri();
    }
}
