package com.gjq.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 算法服务客户端
 */
@Component
public class AlgorithmClient {
    
    private static final Logger logger = LoggerFactory.getLogger(AlgorithmClient.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    // 算法服务请求接口统一前缀为/api，已经在application.yml中配置(http://localhost:5000/api)，禁止修改
    @Value("${algorithm.server.url}")
    private String serverUrl;

    /**
     * 通用响应格式(已经和算法端联调好，禁止修改)
     */
    private static class Response<T> {
        private int code;
        private String msg;
        private T data;
        
        public int getCode() {
            return code;
        }
        
        public String getMsg() {
            return msg;
        }
        
        public T getData() {
            return data;
        }
    }

    /**
     * 处理响应结果
     *
     * @param response 响应对象
     * @return 响应数据
     * @throws RuntimeException 请求失败时抛出异常
     */
    private <T> T handleResponse(Response<T> response) {
        if (response != null && response.getCode() == 200) {
            return response.getData();
        }
        throw new RuntimeException(response != null ? response.getMsg() : "请求失败");
    }

    /**
     * 发送GET请求
     *
     * @param path 请求路径
     * @param params 查询参数
     * @return 响应数据
     * @throws RuntimeException 请求失败时抛出异常
     */
    public <T> T get(String path, Map<String, Object> params) {
        try {
            String url = serverUrl + path;
            ResponseEntity<Response<T>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Response<T>>() {},
                params
            );
            return handleResponse(response.getBody());
        } catch (Exception e) {
            logger.error("GET请求失败", e);
            throw new RuntimeException("GET请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送POST请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 响应数据
     * @throws RuntimeException 请求失败时抛出异常
     */
    public <T> T post(String path, Object body) {
        try {
            String url = serverUrl + path;
            ResponseEntity<Response<T>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<Response<T>>() {}
            );
            return handleResponse(response.getBody());
        } catch (Exception e) {
            logger.error("POST请求失败", e);
            throw new RuntimeException("POST请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送带文件的POST请求
     *
     * @param path 请求路径
     * @param file 文件
     * @param params 其他参数
     * @return 响应数据
     * @throws RuntimeException 请求失败时抛出异常
     */
    public <T> T postWithFile(String path, MultipartFile file, Map<String, Object> params) {
        try {
            String url = serverUrl + path;
            
            // 构建表单数据
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            if (file != null) {
                body.add("file", file.getResource());
            }
            if (params != null) {
                params.forEach((key, value) -> body.add(key, value));
            }

            ResponseEntity<Response<T>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<Response<T>>() {}
            );
            return handleResponse(response.getBody());
        } catch (Exception e) {
            logger.error("POST文件请求失败", e);
            throw new RuntimeException("POST文件请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送PUT请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 响应数据
     * @throws RuntimeException 请求失败时抛出异常
     */
    public <T> T put(String path, Object body) {
        try {
            String url = serverUrl + path;
            ResponseEntity<Response<T>> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<Response<T>>() {}
            );
            return handleResponse(response.getBody());
        } catch (Exception e) {
            logger.error("PUT请求失败", e);
            throw new RuntimeException("PUT请求失败: " + e.getMessage());
        }
    }

    /**
     * 发送DELETE请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 响应数据
     * @throws RuntimeException 请求失败时抛出异常
     */
    public <T> T delete(String path, Object body) {
        try {
            String url = serverUrl + path;
            ResponseEntity<Response<T>> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                new HttpEntity<>(body),
                new ParameterizedTypeReference<Response<T>>() {}
            );
            return handleResponse(response.getBody());
        } catch (Exception e) {
            logger.error("DELETE请求失败", e);
            throw new RuntimeException("DELETE请求失败: " + e.getMessage());
        }
    }
    
} 