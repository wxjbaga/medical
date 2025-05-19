package com.gjq.vo.dataset;

import java.time.LocalDateTime;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gjq.vo.user.UserInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据集VO
 */
@Data
@Schema(description = "数据集VO")
public class DatasetVO {
    
    /**
     * 数据集ID
     */
    @Schema(description = "数据集ID")
    private Long id;
    
    /**
     * 数据集名称
     */
    @Schema(description = "数据集名称")
    private String name;

    /**
     * 数据集描述
     */
    @Schema(description = "数据集描述")
    private String description;

    /**
     * 数据集桶
     */
    @Schema(description = "数据集桶")
    private String bucket;

    /**
     * 数据集对象键
     */
    @Schema(description = "数据集对象键")
    private String objectKey;
    
    /**
     * 训练样例数量
     */
    @Schema(description = "训练样例数量")
    private Integer trainCount;
    
    /**
     * 验证样例数量
     */
    @Schema(description = "验证样例数量")
    private Integer valCount;
    
    /**
     * 状态(0:未验证 1:验证中 2:验证成功 3:验证失败)
     */
    @Schema(description = "状态(0:未验证 1:验证中 2:验证成功 3:验证失败)")
    private Integer status;
    
    /**
     * 状态名称
     */
    @Schema(description = "状态名称")
    private String statusName;
    
    /**
     * 错误消息
     */
    @Schema(description = "错误消息")
    private String errorMsg;
    
    /**
     * 创建用户ID
     */
    @Schema(description = "创建用户ID")
    private Long createUserId;
    
    /**
     * 创建用户
     */
    @Schema(description = "创建用户")
    private UserInfo createUser;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

} 