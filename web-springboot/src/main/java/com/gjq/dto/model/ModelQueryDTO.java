package com.gjq.dto.model;

import com.gjq.dto.PageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模型查询DTO")
public class ModelQueryDTO extends PageDTO {
    
    @Schema(description = "模型名称")
    private String name;
    
    @Schema(description = "状态(0:未训练 1:训练中 2:训练成功 3:训练失败 4:已发布)")
    private Integer status;
    
    @Schema(description = "数据集ID")
    private Long datasetId;
} 