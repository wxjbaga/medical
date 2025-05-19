package com.gjq.dto.dataset;

import com.gjq.dto.PageDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据集查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "数据集查询DTO")
public class DatasetQueryDTO extends PageDTO {
    
    /**
     * 数据集名称
     */
    @Schema(description = "数据集名称")
    private String name;
    
    /**
     * 状态(0:未验证 1:验证中 2:验证成功 3:验证失败)
     */
    @Schema(description = "状态(0:未验证 1:验证中 2:验证成功 3:验证失败)")
    private Integer status;
    
    /**
     * 创建用户ID
     */
    @Schema(description = "创建用户ID")
    private Long createUserId;
} 