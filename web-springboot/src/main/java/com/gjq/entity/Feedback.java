package com.gjq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评估反馈实体类
 */
@Data
@TableName("feedback")
public class Feedback {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模型ID
     */
    private Long modelId;
    
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
    
    /**
     * 创建用户ID
     */
    private Long createUserId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 