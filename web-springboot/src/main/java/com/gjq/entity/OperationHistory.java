package com.gjq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作历史实体类
 */
@Data
@TableName("operation_history")
public class OperationHistory {
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
     * 原图存储桶
     */
    private String originalImageBucket;
    
    /**
     * 原图对象键
     */
    private String originalImageKey;
    
    /**
     * 结果图存储桶
     */
    private String resultImageBucket;
    
    /**
     * 结果图对象键
     */
    private String resultImageKey;
    
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