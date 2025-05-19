"""
常量定义
"""

class StatusConstant:
    """
    数据集状态常量
    与Java端StatusConstant类保持一致
    """
    # 未验证
    DATASET_STATUS_UNVERIFIED = 0  # 与Java端DATASET_STATUS_UNVERIFIED一致
    
    # 验证中
    DATASET_STATUS_VERIFYING = 1  # 与Java端DATASET_STATUS_VERIFYING一致
    
    # 验证成功
    DATASET_STATUS_VERIFIED_SUCCESS = 2  # 与Java端DATASET_STATUS_VERIFIED_SUCCESS一致
    
    # 验证失败
    DATASET_STATUS_VERIFIED_FAILED = 3  # 与Java端DATASET_STATUS_VERIFIED_FAILED一致
    
    # 模型状态: 未训练
    MODEL_STATUS_UNTRAINED = 0  # 与Java端MODEL_STATUS_UNTRAINED一致
    
    # 模型状态: 训练中
    MODEL_STATUS_TRAINING = 1  # 与Java端MODEL_STATUS_TRAINING一致
    
    # 模型状态: 训练成功
    MODEL_STATUS_TRAINED_SUCCESS = 2  # 与Java端MODEL_STATUS_TRAINED_SUCCESS一致
    
    # 模型状态: 训练失败
    MODEL_STATUS_TRAINED_FAILED = 3  # 与Java端MODEL_STATUS_TRAINED_FAILED一致
    
    # 模型状态: 已发布
    MODEL_STATUS_PUBLISHED = 4  # 与Java端MODEL_STATUS_PUBLISHED一致 