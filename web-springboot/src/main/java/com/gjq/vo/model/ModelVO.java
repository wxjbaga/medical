package com.gjq.vo.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型视图对象
 */
@Data
@Schema(description = "模型视图对象")
public class ModelVO {
    
    @Schema(description = "模型ID")
    private Long id;
    
    @Schema(description = "模型名称")
    private String name;
    
    @Schema(description = "模型描述")
    private String description;
    
    @Schema(description = "数据集ID")
    private Long datasetId;
    
    @Schema(description = "数据集名称")
    private String datasetName;
    
    @Schema(description = "数据集存储桶")
    private String datasetBucket;
    
    @Schema(description = "数据集对象键")
    private String datasetObjectKey;
    
    @Schema(description = "模型存储桶")
    private String modelBucket;
    
    @Schema(description = "模型对象键")
    private String modelObjectKey;
    
    @Schema(description = "状态(0:未训练 1:训练中 2:训练成功 3:训练失败 4:已发布)")
    private Integer status;
    
    @Schema(description = "错误信息")
    private String errorMsg;
    
    @Schema(description = "训练超参数(JSON字符串)")
    private String trainHyperparams;
    
    @Schema(description = "训练指标(JSON字符串)")
    private String trainMetrics;
    
    @Schema(description = "创建用户ID")
    private Long createUserId;
    
    @Schema(description = "创建用户名")
    private String createUsername;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 