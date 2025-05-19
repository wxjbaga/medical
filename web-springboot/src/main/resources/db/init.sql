DROP DATABASE IF EXISTS gjq_db;
CREATE DATABASE IF NOT EXISTS gjq_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gjq_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar_bucket` VARCHAR(50) COMMENT '头像存储桶',
    `avatar_object_key` VARCHAR(255) COMMENT '头像对象键',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色(0:普通用户 1:管理员)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0:禁用 1:启用)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 初始化管理员用户(初始密码：123456)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `email`, `role`, `status`) 
VALUES ('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '12345678901', 'admin@example.com', 1, 1);

-- 初始化测试用户(初始密码：123456)
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `email`, `role`, `status`) 
VALUES ('test', 'e10adc3949ba59abbe56e057f20f883e', '测试用户', '12345678902', 'test@example.com', 0, 1);

-- 数据集表
CREATE TABLE IF NOT EXISTS `dataset` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '数据集ID',
    `name` VARCHAR(100) NOT NULL COMMENT '数据集名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '数据集描述',
    `bucket` VARCHAR(50) DEFAULT NULL COMMENT '存储桶',
    `object_key` VARCHAR(255) DEFAULT NULL COMMENT '对象键',
    `train_count` INT DEFAULT 0 COMMENT '训练集样例数量',
    `val_count` INT DEFAULT 0 COMMENT '验证集样例数量',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0:未验证 1:验证中 2:验证成功 3:验证失败)',
    `error_msg` VARCHAR(255) COMMENT '错误信息',
    `create_user_id` BIGINT NOT NULL COMMENT '创建用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据集表'; 

-- 模型表
DROP TABLE IF EXISTS `model`;
CREATE TABLE IF NOT EXISTS `model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) NOT NULL COMMENT '模型名称',
  `description` varchar(500) DEFAULT NULL COMMENT '模型描述',
  `dataset_id` bigint NOT NULL COMMENT '训练数据集ID',
  `dataset_bucket` varchar(100) DEFAULT NULL COMMENT '训练数据集存储桶(复制的数据集文件)',
  `dataset_object_key` varchar(200) DEFAULT NULL COMMENT '训练数据集对象键(复制的数据集文件)',
  `model_bucket` varchar(100) DEFAULT NULL COMMENT '模型权重存储桶',
  `model_object_key` varchar(200) DEFAULT NULL COMMENT '模型权重对象键',
  `status` tinyint DEFAULT 0 COMMENT '状态：0-未训练，1-训练中，2-训练成功，3-训练失败，4-已发布',
  `error_msg` VARCHAR(255) COMMENT '错误信息',
  `train_hyperparams` TEXT COMMENT '超参数(JSON格式，包含epochs、batch_size等训练参数)',
  `train_metrics` TEXT COMMENT '评估指标(JSON格式，包含dice、iou等评估指标)',
  `create_user_id` BIGINT NOT NULL COMMENT '创建用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dataset_id` (`dataset_id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型表';

-- 评估反馈表
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE IF NOT EXISTS `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_id` bigint NOT NULL COMMENT '模型ID',
  `content` varchar(1000) DEFAULT NULL COMMENT '反馈内容',
  `score` int NOT NULL COMMENT '评分(1-5)',
  `metrics` text COMMENT '评估指标(JSON格式)',
  `original_image_bucket` varchar(100) DEFAULT NULL COMMENT '原图存储桶',
  `original_image_key` varchar(200) DEFAULT NULL COMMENT '原图对象键',
  `label_image_bucket` varchar(100) DEFAULT NULL COMMENT '标签图存储桶',
  `label_image_key` varchar(200) DEFAULT NULL COMMENT '标签图对象键',
  `overlay_image_bucket` varchar(100) DEFAULT NULL COMMENT '叠加图存储桶',
  `overlay_image_key` varchar(200) DEFAULT NULL COMMENT '叠加图对象键',
  `create_user_id` bigint NOT NULL COMMENT '创建用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估反馈表';

-- 操作历史表
DROP TABLE IF EXISTS `operation_history`;
CREATE TABLE IF NOT EXISTS `operation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_id` bigint NOT NULL COMMENT '模型ID',
  `original_image_bucket` varchar(100) DEFAULT NULL COMMENT '原图存储桶',
  `original_image_key` varchar(200) DEFAULT NULL COMMENT '原图对象键',
  `result_image_bucket` varchar(100) DEFAULT NULL COMMENT '结果图存储桶',
  `result_image_key` varchar(200) DEFAULT NULL COMMENT '结果图对象键',
  `overlay_image_bucket` varchar(100) DEFAULT NULL COMMENT '叠加图存储桶',
  `overlay_image_key` varchar(200) DEFAULT NULL COMMENT '叠加图对象键',
  `create_user_id` bigint NOT NULL COMMENT '创建用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_model_id` (`model_id`),
  KEY `idx_create_user_id` (`create_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作历史表';