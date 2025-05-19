package com.gjq.common;

/**
 * 数据集状态常量类
 * 定义了数据集在不同验证阶段的状态码
 */
public class StatusConstant {
    
    /**
     * 数据集状态 - 未验证
     * 表示数据集刚刚创建，还未进行格式验证
     */
    public static final int DATASET_STATUS_UNVERIFIED = 0;
    
    /**
     * 数据集状态 - 验证中
     * 表示数据集正在进行格式验证
     */
    public static final int DATASET_STATUS_VERIFYING = 1;
    
    /**
     * 数据集状态 - 验证成功
     * 表示数据集通过了格式验证，可以用于训练模型
     */
    public static final int DATASET_STATUS_VERIFIED_SUCCESS = 2;
    
    /**
     * 数据集状态 - 验证失败
     * 表示数据集未通过格式验证，需要重新上传
     */
    public static final int DATASET_STATUS_VERIFIED_FAILED = 3;
    
    /**
     * 模型状态: 未训练
     */
    public static final int MODEL_STATUS_UNTRAINED = 0;
    
    /**
     * 模型状态: 训练中
     */
    public static final int MODEL_STATUS_TRAINING = 1;
    
    /**
     * 模型状态: 训练成功
     */
    public static final int MODEL_STATUS_TRAINED_SUCCESS = 2;
    
    /**
     * 模型状态: 训练失败
     */
    public static final int MODEL_STATUS_TRAINED_FAILED = 3;
    
    /**
     * 模型状态: 已发布
     */
    public static final int MODEL_STATUS_PUBLISHED = 4;
} 