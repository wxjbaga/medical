package com.gjq.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型实体类
 */
@Data
@TableName("model")
public class Model {
    
    /**
     * 模型ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 模型名称
     */
    private String name;
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * 数据集ID
     */
    private Long datasetId;
    
    /**
     * 数据集存储桶
     */
    private String datasetBucket;
    
    /**
     * 数据集对象键
     */
    private String datasetObjectKey;
    
    /**
     * 模型存储桶
     */
    private String modelBucket;
    
    /**
     * 模型对象键
     */
    private String modelObjectKey;
    
    /**
     * 状态(0:未训练 1:训练中 2:训练成功 3:训练失败 4:已发布)
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 训练超参数(JSON字符串)
     */
    private String trainHyperparams;
    
    /**
     * 训练指标(JSON字符串)
     */
    private String trainMetrics;
    
    /**
     * 创建用户ID
     */
    private Long createUserId;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
} 