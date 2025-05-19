package com.gjq.dto.dataset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据集添加DTO
 */
@Data
@Schema(description = "数据集添加DTO")
public class DatasetAddDTO {
    
    /**
     * 数据集名称
     */
    @NotBlank(message = "数据集名称不能为空")
    @Size(max = 100, message = "数据集名称长度不能超过100个字符")
    @Schema(description = "数据集名称", required = true)
    private String name;
    
    /**
     * 数据集描述
     */
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    
} 