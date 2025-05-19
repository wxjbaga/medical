package com.gjq.dto.feedback;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评估反馈视图对象
 */
@Data
public class FeedbackVO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 模型ID
     */
    private Long modelId;
    
    /**
     * 模型名称
     */
    private String modelName;
    
    /**
     * 反馈内容
     */
    private String content;
    
    /**
     * 评分(1-5)
     */
    private Integer score;
    
    /**
     * 评估指标(JSON格式)
     */
    private String metrics;
    
    /**
     * 原图URL
     */
    private String originalImageUrl;
    
    /**
     * 标签图URL
     */
    private String labelImageUrl;
    
    /**
     * 叠加图URL
     */
    private String overlayImageUrl;
    
    /**
     * 创建用户ID
     */
    private Long createUserId;
    
    /**
     * 创建用户名称
     */
    private String createUserName;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 