"""
数据集服务客户端
"""
import logging
from clients.springboot_client import springboot_client

logger = logging.getLogger(__name__)

class DatasetClient:
    """数据集服务客户端"""
    
    def __init__(self):
        self.client = springboot_client
    
    def get_dataset_info(self, dataset_id):
        """
        获取数据集信息
        
        Args:
            dataset_id: 数据集ID
            
        Returns:
            dict: 数据集信息，失败返回None
        """
        try:
            # 构建请求路径
            path = f"/dataset/detail/{dataset_id}"
            
            # 发送GET请求
            return self.client.get(path)
            
        except Exception as e:
            logger.error(f"获取数据集信息时发生错误: {e}")
            return None
    
    def update_dataset_status(self, dataset_id, status, error_msg=None, train_count=0, val_count=0):
        """
        更新数据集验证状态
        
        Args:
            dataset_id: 数据集ID
            status: 状态码 (1-未验证, 2-验证中, 3-验证成功, 4-验证失败)
            error_msg: 错误信息，验证失败时使用
            train_count: 训练样本数量
            val_count: 验证样本数量
            
        Returns:
            bool: 更新是否成功
        """
        try:
            # 构建请求路径 - 修改为正确的API路径
            path = "/dataset/update-status"
            
            # 准备请求数据
            data = {
                "id": dataset_id,
                "status": status,
                "errorMsg": error_msg,
                "trainCount": train_count,
                "valCount": val_count
            }
            
            # 发送POST请求
            self.client.post(path, json=data)
            logger.info(f"数据集 {dataset_id} 状态已更新为 {status}")
            return True
            
        except Exception as e:
            logger.error(f"更新数据集状态时发生错误: {e}")
            return False

# 创建全局实例
dataset_client = DatasetClient() 