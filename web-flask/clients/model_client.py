"""
模型服务客户端
"""
import logging
from clients.springboot_client import springboot_client

logger = logging.getLogger(__name__)

class ModelClient:
    """模型服务客户端"""
    
    def __init__(self):
        self.client = springboot_client
    
    def get_model_info(self, model_id):
        """
        获取模型信息
        
        Args:
            model_id: 模型ID
            
        Returns:
            dict: 模型信息，失败返回None
        """
        try:
            # 构建请求路径
            path = f"/model/detail/{model_id}"
            
            # 发送GET请求
            return self.client.get(path)
            
        except Exception as e:
            logger.error(f"获取模型信息时发生错误: {e}")
            return None
    
    def update_model_status(self, model_id, status, error_msg=None, train_metrics=None, 
                           model_bucket=None, model_object_key=None):
        """
        更新模型状态
        
        Args:
            model_id: 模型ID
            status: 状态码 (0-未训练, 1-训练中, 2-训练成功, 3-训练失败, 4-已发布)
            error_msg: 错误信息
            train_metrics: 训练指标(JSON字符串)
            model_bucket: 模型文件存储桶
            model_object_key: 模型文件对象键
            
        Returns:
            bool: 更新是否成功
        """
        try:
            # 构建请求路径
            path = "/model/update-status"
            
            # 准备请求数据
            data = {
                "id": model_id,
                "status": status
            }
            
            # 添加可选参数
            if error_msg is not None:
                data["errorMsg"] = error_msg
                
            if train_metrics is not None:
                data["trainMetrics"] = train_metrics
                
            if model_bucket is not None:
                data["modelBucket"] = model_bucket
                
            if model_object_key is not None:
                data["modelObjectKey"] = model_object_key
            
            # 发送POST请求
            self.client.post(path, json=data)
            logger.info(f"模型 {model_id} 状态已更新为 {status}")
            return True
            
        except Exception as e:
            logger.error(f"更新模型状态时发生错误: {e}")
            return False

# 创建全局实例
model_client = ModelClient() 