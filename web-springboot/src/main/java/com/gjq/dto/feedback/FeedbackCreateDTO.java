package com.gjq.dto.feedback;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 评估反馈创建DTO
 */
@Data
public class FeedbackCreateDTO {
    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空")
    private Long modelId;
    
    /**
     * 反馈内容
     */
    private String content;
    
    /**
     * 评分(1-5)
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能小于1")
    @Max(value = 5, message = "评分不能大于5")
    private Integer score;
    
    /**
     * 评估指标(JSON格式)
     */
    private String metrics;
    
    /**
     * 原图存储桶
     */
    private String originalImageBucket;
    
    /**
     * 原图对象键
     */
    private String originalImageKey;
    
    /**
     * 标签图存储桶
     */
    private String labelImageBucket;
    
    /**
     * 标签图对象键
     */
    private String labelImageKey;
    
    /**
     * 叠加图存储桶
     */
    private String overlayImageBucket;
    
    /**
     * 叠加图对象键
     */
    private String overlayImageKey;
} 