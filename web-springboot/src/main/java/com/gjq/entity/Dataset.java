package com.gjq.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据集实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("dataset")
public class Dataset {
    
    /**
     * 数据集ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据集名称
     */
    private String name;
    
    /**
     * 存储桶
     */
    private String bucket;
    
    /**
     * 对象键
     */
    private String objectKey;
    
    /**
     * 训练样例数量
     */
    private Integer trainCount;
    
    /**
     * 验证样例数量
     */
    private Integer valCount;
    
    /**
     * 状态(0:未验证 1:验证中 2:验证成功 3:验证失败)
     */
    private Integer status;
    
    /**
     * 错误消息
     */
    private String errorMsg;
    
    /**
     * 创建用户ID
     */
    @TableField("create_user_id")
    private Long createUserId;
    
    /**
     * 数据集描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建用户（非数据库字段）
     */
    @TableField(exist = false)
    private User createUser;
} 