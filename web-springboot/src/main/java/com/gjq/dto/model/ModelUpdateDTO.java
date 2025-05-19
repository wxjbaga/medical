package com.gjq.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模型更新DTO
 */
@Data
@Schema(description = "模型更新DTO")
public class ModelUpdateDTO {
    
    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID", required = true)
    private Long id;
    
    @Schema(description = "模型名称")
    private String name;
    
    @Schema(description = "模型描述")
    private String description;
    
    @Schema(description = "数据集ID")
    private Long datasetId;
} 