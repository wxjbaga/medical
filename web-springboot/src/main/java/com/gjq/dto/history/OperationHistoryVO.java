package com.gjq.dto.history;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作历史视图对象
 */
@Data
@Schema(description = "操作历史视图对象")
public class OperationHistoryVO {

    @Schema(description = "操作历史ID")
    private Long id;
    
    @Schema(description = "模型ID")
    private Long modelId;
    
    @Schema(description = "模型名称")
    private String modelName;
    
    @Schema(description = "原始图片URL")
    private String originalImageUrl;
    
    @Schema(description = "结果图片URL")
    private String resultImageUrl;
    
    @Schema(description = "叠加图片URL")
    private String overlayImageUrl;
    
    @Schema(description = "创建用户ID")
    private Long createUserId;
    
    @Schema(description = "创建用户名称")
    private String createUserName;
    
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
} 