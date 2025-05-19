"""
数据集相关路由
"""
import os
import uuid
from flask import Blueprint, request
from utils.dataset_util import validate_dataset
from common.constants import StatusConstant
from clients.file_client import file_client
from clients.dataset_client import dataset_client
from utils.response import success, error
import logging
from config import TEMP_DIR

# 创建蓝图
dataset_bp = Blueprint('dataset', __name__)
logger = logging.getLogger(__name__)

@dataset_bp.route('/validate', methods=['POST'])
def validate_dataset_api():
    """
    数据集验证接口
    
    请求参数:
        - id: 数据集ID
    
    返回:
        - JSON: 包含验证结果的JSON对象
    """
    try:
        # 获取请求数据
        data = request.json or {}
        dataset_id = data.get('id')
            
        if not dataset_id:
            return error('缺少必要参数：id', 400)
            
        try:
            dataset_id = int(dataset_id)
        except ValueError:
            return error('id必须是整数', 400)
        
        # 更新数据集状态为"验证中"
        update_success = dataset_client.update_dataset_status(dataset_id, StatusConstant.DATASET_STATUS_VERIFYING)
        if not update_success:
            return error('更新数据集状态失败', 500)
        
        # 获取数据集信息
        dataset_info = dataset_client.get_dataset_info(dataset_id)
        if not dataset_info:
            # 更新数据集状态为"验证失败"
            dataset_client.update_dataset_status(dataset_id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, "无法获取数据集信息")
            return error('无法获取数据集信息', 400, {
                'valid': False,
                'error_msg': '无法获取数据集信息',
                'train_count': 0,
                'val_count': 0
            })
            
        # 获取数据集的存储信息
        bucket = dataset_info.get('bucket')
        object_key = dataset_info.get('objectKey')
        
        if not bucket or not object_key:
            # 更新数据集状态为"验证失败"
            dataset_client.update_dataset_status(dataset_id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, "数据集存储信息不完整")
            return error('数据集存储信息不完整', 400, {
                'valid': False,
                'error_msg': '数据集存储信息不完整',
                'train_count': 0,
                'val_count': 0
            })
            
        # 使用配置的临时目录创建临时文件
        temp_filename = f"dataset_{dataset_id}_{uuid.uuid4().hex}.zip"
        temp_path = os.path.join(TEMP_DIR, temp_filename)
            
        # 下载数据集文件
        try:
            file_client.download(bucket, object_key, temp_path)
            download_success = True
        except Exception as e:
            logger.error(f"下载数据集文件失败: {e}")
            download_success = False
            
        if not download_success:
            # 清理临时文件
            if os.path.exists(temp_path):
                os.remove(temp_path)
                
            # 更新数据集状态为"验证失败"
            dataset_client.update_dataset_status(dataset_id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, "下载数据集文件失败")
            return error('下载数据集文件失败', 400, {
                'valid': False,
                'error_msg': '下载数据集文件失败',
                'train_count': 0,
                'val_count': 0
            })
            
        # 验证数据集
        validation_result = validate_dataset(temp_path)
        logger.info(f"数据集验证结果: {validation_result}")
        
        # 删除临时文件
        try:
            if os.path.exists(temp_path):
                os.remove(temp_path)
        except Exception as e:
            logger.error(f"删除临时文件失败: {e}")
            
        # 如果验证成功
        if validation_result['success']:
            # 更新数据集状态为"验证成功"
            dataset_client.update_dataset_status(
                dataset_id, 
                StatusConstant.DATASET_STATUS_VERIFIED_SUCCESS,
                None,
                validation_result.get('train_count', 0),
                validation_result.get('val_count', 0)
            )
            # 返回数据格式需与后端期望格式一致
            return success({
                'valid': True,
                'error_msg': None,
                'train_count': validation_result.get('train_count', 0),
                'val_count': validation_result.get('val_count', 0)
            }, '数据集验证成功')
        else:
            # 更新数据集状态为"验证失败"
            error_msg = validation_result.get('error', '验证失败，未知错误')
            dataset_client.update_dataset_status(
                dataset_id, 
                StatusConstant.DATASET_STATUS_VERIFIED_FAILED, 
                error_msg
            )
            # 返回数据格式需与后端期望格式一致
            return error(error_msg, 400, {
                'valid': False,
                'error_msg': error_msg,
                'train_count': 0,
                'val_count': 0
            })
                
    except Exception as e:
        logger.error(f"验证数据集时发生错误: {e}")
        # 清理临时文件
        if 'temp_path' in locals() and os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except:
                pass
                
        # 更新数据集状态为"验证失败"
        if 'dataset_id' in locals():
            dataset_client.update_dataset_status(dataset_id, StatusConstant.DATASET_STATUS_VERIFIED_FAILED, f"验证数据集时发生错误: {str(e)}")
        return error(f'服务器内部错误: {str(e)}', 500, {
            'valid': False,
            'error_msg': f'服务器内部错误: {str(e)}',
            'train_count': 0,
            'val_count': 0
        }) 