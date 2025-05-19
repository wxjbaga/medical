package com.gjq.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "用户状态DTO")
public class UserStatusDTO {
    
    @NotNull(message = "状态不能为空")
    @Schema(description = "状态：0-禁用，1-正常")
    private Integer status;
} 