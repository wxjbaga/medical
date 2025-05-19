package com.gjq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询基础DTO
 */
@Data
@Schema(description = "分页查询基础DTO")
public class PageDTO {
    /**
     * 当前页码
     */
    @Schema(description = "当前页码", defaultValue = "1")
    private Integer current = 1;

    /**
     * 每页记录数
     */
    @Schema(description = "每页记录数", defaultValue = "10")
    private Integer size = 10;
} 