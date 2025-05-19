package com.gjq.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.LinkedHashMap;

/**
 * 文件对象存储服务客户端
 */
@Component
public class FileClient {
    
    private static final Logger logger = LoggerFactory.getLogger(FileClient.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${file.server.url}")
    private String serverUrl;

    /**
     * 通用响应格式
     */
    public static class Response<T> {
        private int code;
        private String msg;
        private T data;
        
        public int getCode() {
            return code;
        }
        
        public void setCode(int code) {
            this.code = code;
        }
        
        public String getMsg() {
            return msg;
        }
        
        public void setMsg(String msg) {
            this.msg = msg;
        }
        
        public T getData() {
            return data;
        }
        
        public void setData(T data) {
            this.data = data;
        }
    }
    
    /**
     * 文件上传响应数据
     */
    public static class UploadResult {
        private String url;
        private String bucket;
        private String objectKey;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getBucket() {
            return bucket;
        }
        
        public void setBucket(String bucket) {
            this.bucket = bucket;
        }
        
        public String getObjectKey() {
            return objectKey;
        }
        
        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey;
        }
    }
    
    /**
     * 上传文件
     *
     * @param bucket 存储桶名称
     * @param file 文件
     * @param isCache 是否缓存，默认false
     * @return 上传结果
     * @throws RuntimeException 上传失败时抛出异常
     */
    public UploadResult upload(String bucket, MultipartFile file, boolean isCache) {
        try {
            String url = serverUrl + "/upload/" + bucket;
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // 设置请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("is_cache", isCache);
            
            // 发送请求
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Response<LinkedHashMap<String, Object>> response = (Response<LinkedHashMap<String, Object>>) restTemplate.postForObject(
                url,
                requestEntity,
                Response.class
            );
            
            // 检查响应
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                LinkedHashMap<String, Object> data = response.getData();
                UploadResult result = new UploadResult();
                result.setUrl((String) data.get("url"));
                result.setBucket((String) data.get("bucket"));
                result.setObjectKey((String) data.get("objectKey"));
                return result;
            }
            throw new RuntimeException(response != null ? response.getMsg() : "上传失败");
            
        } catch (Exception e) {
            logger.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传文件
     *
     * @param bucket 存储桶名称
     * @param fileContent 文件内容
     * @param fileName 文件名
     * @param isCache 是否缓存，默认false
     * @return 上传结果
     * @throws RuntimeException 上传失败时抛出异常
     */
    public UploadResult upload(String bucket, byte[] fileContent, String fileName, boolean isCache) {
        try {
            String url = serverUrl + "/upload/" + bucket;
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // 设置请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileContent) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            });
            body.add("is_cache", isCache);
            
            // 发送请求
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Response<LinkedHashMap<String, Object>> response = (Response<LinkedHashMap<String, Object>>) restTemplate.postForObject(
                url,
                requestEntity,
                Response.class
            );
            
            // 检查响应
            if (response != null && response.getCode() == 200 && response.getData() != null) {
                LinkedHashMap<String, Object> data = response.getData();
                UploadResult result = new UploadResult();
                result.setUrl((String) data.get("url"));
                result.setBucket((String) data.get("bucket"));
                result.setObjectKey((String) data.get("objectKey"));
                return result;
            }
            throw new RuntimeException(response != null ? response.getMsg() : "上传失败");
            
        } catch (Exception e) {
            logger.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     *
     * @param bucket 存储桶名称
     * @param objectKey 对象键
     * @throws RuntimeException 删除失败时抛出异常
     */
    public void delete(String bucket, String objectKey) {
        String url = serverUrl + "/" + bucket + "/" + objectKey;
        Response<?> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.DELETE, null, Response.class).getBody();
        
        // 检查响应
        if (response == null || response.getCode() != 200) {
            throw new RuntimeException(response != null ? response.getMsg() : "删除失败");
        }
    }
    
    /**
     * 获取文件
     *
     * @param bucket 存储桶名称
     * @param objectKey 对象键
     * @return 文件字节数组
     * @throws RuntimeException 获取失败时抛出异常
     */
    public byte[] get(String bucket, String objectKey) {
        try {
            String url = serverUrl + "/" + bucket + "/" + objectKey;
            return restTemplate.getForObject(url, byte[].class);
        } catch (Exception e) {
            logger.error("获取文件失败", e);
            throw new RuntimeException("获取文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件URL
     *
     * @param bucket 存储桶名称
     * @param objectKey 对象键
     * @return 文件URL
     */
    public String getFileUrl(String bucket, String objectKey) {
        return String.format("%s/%s/%s", serverUrl, bucket, objectKey);
    }
} 