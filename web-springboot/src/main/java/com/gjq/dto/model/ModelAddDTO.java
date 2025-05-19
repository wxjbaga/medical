package com.gjq.dto.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 模型添加DTO
 */
@Data
@Schema(description = "模型添加DTO")
public class ModelAddDTO {
    
    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", required = true)
    private String name;
    
    @Schema(description = "模型描述")
    private String description;
    
    @NotNull(message = "数据集ID不能为空")
    @Schema(description = "数据集ID", required = true)
    private Long datasetId;
    
    @Schema(description = "训练超参数(JSON字符串)", required = true)
    private String trainHyperparams;
} 