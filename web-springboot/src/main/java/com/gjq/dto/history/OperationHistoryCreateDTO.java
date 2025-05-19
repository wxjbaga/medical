package com.gjq.dto.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 操作历史创建DTO
 */
@Data
@Schema(description = "操作历史创建参数")
public class OperationHistoryCreateDTO {
    
    /**
     * 模型ID
     */
    @NotNull(message = "模型ID不能为空")
    @Schema(description = "模型ID", required = true)
    private Long modelId;
    
    /**
     * 原图存储桶
     */
    @Schema(description = "原始图片的桶名称")
    private String originalImageBucket;
    
    /**
     * 原图对象键
     */
    @Schema(description = "原始图片的对象键")
    private String originalImageKey;
    
    /**
     * 结果图存储桶
     */
    @Schema(description = "结果图片的桶名称")
    private String resultImageBucket;
    
    /**
     * 结果图对象键
     */
    @Schema(description = "结果图片的对象键")
    private String resultImageKey;
    
    /**
     * 叠加图存储桶
     */
    @Schema(description = "叠加图片的桶名称")
    private String overlayImageBucket;
    
    /**
     * 叠加图对象键
     */
    @Schema(description = "叠加图片的对象键")
    private String overlayImageKey;
} 