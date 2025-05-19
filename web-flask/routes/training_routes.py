"""
模型训练相关路由
"""
import os
import json
import time
import threading
import traceback
from typing import Dict, List, Any, Optional, Union
from flask import Blueprint, request, jsonify
import logging
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
import numpy as np
import glob
import shutil
import zipfile
from tqdm import tqdm

# MONAI相关导入
from monai.data import Dataset
from monai.transforms import (
    Compose, LoadImaged, EnsureChannelFirstd, ScaleIntensityd, 
    RandRotate90d, ToTensord, Resized
)
from monai.networks.nets import UNet
from monai.losses import DiceLoss
from monai.metrics import DiceMetric
from monai.utils import set_determinism
from monai.optimizers import Novograd

from config import TEMP_DIR  # 导入配置中的临时目录
from clients.file_client import file_client
from clients.model_client import model_client
from common.constants import StatusConstant  # 使用常量类
from utils.response import api_response, success, error  # 导入统一响应函数

# 创建日志记录器
logger = logging.getLogger(__name__)

# 创建蓝图
training_bp = Blueprint('training', __name__)

# 全局训练状态
training_threads = {}

# 辅助函数：安全转换数值为浮点数，处理列表和其他类型
def safe_float_convert(value: Any) -> Union[float, List, Dict, Any]:
    """安全地将值转换为浮点数，如果是列表或字典则递归处理"""
    if isinstance(value, (int, float)):
        return float(value)
    elif isinstance(value, str):
        try:
            return float(value)
        except ValueError:
            return value
    elif isinstance(value, list):
        return [safe_float_convert(item) for item in value]
    elif isinstance(value, dict):
        return {k: safe_float_convert(v) for k, v in value.items()}
    else:
        # 其他类型保持不变
        return value

# 服务启动时检查未完成的训练任务
def check_unfinished_training_tasks():
    """
    检查未完成的训练任务，将意外中断的任务状态设为失败
    """
    try:
        # 检查TEMP_DIR目录下的所有training_status_{model_id}.lock文件
        status_files = glob.glob(os.path.join(TEMP_DIR, "training_status_*.lock"))
        
        for status_file in status_files:
            try:
                # 从文件名提取模型ID
                filename = os.path.basename(status_file)
                model_id = int(filename.replace("training_status_", "").replace(".lock", ""))
                
                # 获取模型当前状态
                model_info = model_client.get_model_info(model_id)
                
                # 如果模型状态为训练中，说明训练意外中断
                if model_info and model_info.get('status') == StatusConstant.MODEL_STATUS_TRAINING:
                    logger.warning(f"检测到模型 {model_id} 的训练任务意外中断，更新状态为训练失败")
                    
                    # 更新模型状态为训练失败
                    model_client.update_model_status(
                        model_id=model_id,
                        status=StatusConstant.MODEL_STATUS_TRAINED_FAILED,
                        error_msg="训练任务意外中断"
                    )
                
                # 删除状态文件
                os.remove(status_file)
            except Exception as e:
                logger.error(f"处理未完成训练任务时出错: {str(e)}")
    except Exception as e:
        logger.error(f"检查未完成训练任务时出错: {str(e)}")

# 启动应用时检查未完成的训练任务
check_unfinished_training_tasks()

def train_model_thread(
    model_id: int,
    dataset_bucket: str,
    dataset_object_key: str,
    input_size: int,
    in_channels: int,
    num_classes: int,
    epochs: int = 50,
    batch_size: int = 8,
    learning_rate: float = 1e-4,
    aug_level: str = 'medium'
):
    """
    模型训练线程函数
    
    Args:
        model_id: 模型ID
        dataset_bucket: 数据集存储桶
        dataset_object_key: 数据集对象键
        input_size: 输入图像大小
        in_channels: 输入通道数
        num_classes: 类别数量
        epochs: 训练轮数
        batch_size: 批量大小
        learning_rate: 学习率
        aug_level: 数据增强级别
    """
    # 创建训练状态文件，用于检测训练是否意外中断
    status_file = os.path.join(TEMP_DIR, f"training_status_{model_id}.lock")
    try:
        # 设置随机种子以保证可重复性
        set_determinism(seed=42)
        
        # 创建状态文件
        with open(status_file, 'w') as f:
            f.write(f"训练开始时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        # 更新模型状态为训练中
        model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINING)
        
        logger.info(f"模型 {model_id} 开始训练...")
        
        # 创建临时目录 - 使用配置的TEMP_DIR
        dataset_dir = os.path.join(TEMP_DIR, f"dataset_{model_id}")
        model_dir = os.path.join(TEMP_DIR, f"model_{model_id}")
        
        os.makedirs(dataset_dir, exist_ok=True)
        os.makedirs(model_dir, exist_ok=True)
        
        # 下载数据集
        dataset_path = os.path.join(dataset_dir, "dataset.zip")
        logger.info(f"下载数据集 {dataset_bucket}/{dataset_object_key} 到 {dataset_path}")
        
        try:
            # 下载文件 - 修正函数调用方式
            file_client.download(dataset_bucket, dataset_object_key, dataset_path)
            logger.info(f"数据集下载成功: {dataset_path}")
        except Exception as e:
            error_msg = f"下载数据集失败: {str(e)}"
            logger.error(error_msg)
            model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
            os.remove(status_file)  # 删除状态文件
            return
        
        # 解压数据集
        with zipfile.ZipFile(dataset_path, 'r') as zip_ref:
            zip_ref.extractall(dataset_dir)
        
        # 查找数据集根目录，检查train和val目录结构
        dataset_root = dataset_dir
        
        # 检查是否存在单一根目录
        entries = os.listdir(dataset_dir)
        if len(entries) == 1 and os.path.isdir(os.path.join(dataset_dir, entries[0])):
            # 使用该目录作为数据集根目录
            dataset_root = os.path.join(dataset_dir, entries[0])
        
        # 检查目录结构
        if not os.path.isdir(os.path.join(dataset_root, 'train')) or not os.path.isdir(os.path.join(dataset_root, 'val')):
            # 尝试在目录中查找train和val目录
            train_dir = None
            val_dir = None
            
            for root, dirs, files in os.walk(dataset_dir):
                if os.path.basename(root) == 'train':
                    train_dir = root
                elif os.path.basename(root) == 'val':
                    val_dir = root
            
            if train_dir and val_dir:
                # 使用找到的train和val目录的父目录作为数据集根目录
                dataset_root = os.path.dirname(train_dir)
                logger.info(f"找到数据集目录结构: {dataset_root}")
            else:
                error_msg = "无法识别数据集目录结构，请确保数据集包含train和val目录"
                logger.error(error_msg)
                model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
                os.remove(status_file)  # 删除状态文件
                return
        
        # 检查train和val目录下的images和labels目录
        train_images_dir = os.path.join(dataset_root, 'train', 'images')
        train_labels_dir = os.path.join(dataset_root, 'train', 'labels')
        val_images_dir = os.path.join(dataset_root, 'val', 'images')
        val_labels_dir = os.path.join(dataset_root, 'val', 'labels')
        
        if not all(os.path.isdir(d) for d in [train_images_dir, train_labels_dir, val_images_dir, val_labels_dir]):
            error_msg = "数据集目录结构不正确，请确保train和val目录下分别包含images和labels目录"
            logger.error(error_msg)
            model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
            os.remove(status_file)  # 删除状态文件
            return
        
        logger.info(f"数据集目录结构验证通过: {dataset_root}")
        
        # 准备数据集
        train_images = sorted([os.path.join(train_images_dir, f) for f in os.listdir(train_images_dir) if f.endswith((".png", ".jpg", ".jpeg"))])
        train_labels = sorted([os.path.join(train_labels_dir, f) for f in os.listdir(train_labels_dir) if f.endswith((".png", ".jpg", ".jpeg"))])
        val_images = sorted([os.path.join(val_images_dir, f) for f in os.listdir(val_images_dir) if f.endswith((".png", ".jpg", ".jpeg"))])
        val_labels = sorted([os.path.join(val_labels_dir, f) for f in os.listdir(val_labels_dir) if f.endswith((".png", ".jpg", ".jpeg"))])
        
        # 创建数据字典
        train_data = [{"image": img, "label": seg} for img, seg in zip(train_images, train_labels)]
        val_data = [{"image": img, "label": seg} for img, seg in zip(val_images, val_labels)]
        
        # 根据增强级别设置数据增强参数
        aug_params = {}
        if aug_level == "low":
            aug_params = {"prob": 0.3}
        elif aug_level == "medium":
            aug_params = {"prob": 0.5}
        elif aug_level == "high":
            aug_params = {"prob": 0.7}
        else:
            aug_params = {"prob": 0.5}  # 默认中等级别
        
        # 定义数据预处理
        train_transforms = Compose([
            LoadImaged(keys=["image", "label"]),
            EnsureChannelFirstd(keys=["image", "label"]),
            Resized(keys=["image", "label"], spatial_size=(input_size, input_size), mode="nearest"),
            ScaleIntensityd(keys=["image"]),
            RandRotate90d(keys=["image", "label"], **aug_params),
            ToTensord(keys=["image", "label"])
        ])
        
        val_transforms = Compose([
            LoadImaged(keys=["image", "label"]),
            EnsureChannelFirstd(keys=["image", "label"]),
            Resized(keys=["image", "label"], spatial_size=(input_size, input_size), mode="nearest"),
            ScaleIntensityd(keys=["image"]),
            ToTensord(keys=["image", "label"])
        ])
        
        # 创建数据集和数据加载器
        train_dataset = Dataset(data=train_data, transform=train_transforms)
        val_dataset = Dataset(data=val_data, transform=val_transforms)
        
        train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True, num_workers=0)
        val_loader = DataLoader(val_dataset, batch_size=batch_size, shuffle=False, num_workers=0)
        
        if len(train_loader) == 0 or len(val_loader) == 0:
            error_msg = "数据集为空或无法正确加载"
            logger.error(error_msg)
            model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
            os.remove(status_file)  # 删除状态文件
            return
        
        logger.info(f"训练集样本数: {len(train_dataset)}")
        logger.info(f"验证集样本数: {len(val_dataset)}")
        
        # 检查是否有可用的GPU
        device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        logger.info(f"使用设备: {device}")
        
        # 创建模型 - 使用默认的UNet模型
        model = UNet(
            spatial_dims=2,  # 2D 图像
            in_channels=in_channels,
            out_channels=num_classes if num_classes > 2 else 1,  # 二分类使用1个通道 + sigmoid
            channels=(16, 32, 64, 128, 256),  # 通道数
            strides=(2, 2, 2, 2),  # 下采样步长
            num_res_units=2,  # 残差单元数
        ).to(device)
        
        # 定义损失函数 - 使用默认的DiceLoss
        loss_function = DiceLoss(
            include_background=True, 
            sigmoid=num_classes <= 2,  # 二分类使用sigmoid
            softmax=num_classes > 2    # 多分类使用softmax
        )
            
        # 定义优化器
        optimizer = Novograd(model.parameters(), lr=learning_rate)
        
        # 评估指标
        dice_metric = DiceMetric(include_background=True, reduction="mean")
        
        # 训练信息
        best_dice = 0.0
        best_metrics = {}
        train_history = {
            'train_loss': [],
            'val_loss': [],
            'metrics': []
        }
        
        # 开始训练
        logger.info(f"开始训练，总轮数: {epochs}")
        
        for epoch in range(epochs):
            try:
                logger.info(f"Epoch {epoch+1}/{epochs}")
                
                # 训练阶段
                model.train()
                epoch_loss = 0
                for batch_data in tqdm(train_loader, desc=f"Epoch {epoch + 1}/{epochs} Training"):
                    inputs, labels = batch_data["image"].to(device), batch_data["label"].to(device)
                    
                    # 将标签值从 [0, 255] 映射到 [0, 1]
                    if num_classes <= 2:
                        labels = labels / 255.0
                    
                    optimizer.zero_grad()
                    outputs = model(inputs)
                    loss = loss_function(outputs, labels)
                    loss.backward()
                    optimizer.step()
                    
                    epoch_loss += loss.item()
                
                train_loss = epoch_loss / len(train_loader)
                logger.info(f"训练损失: {train_loss:.4f}")
                
                # 验证阶段
                model.eval()
                val_loss = 0
                with torch.no_grad():
                    for batch_data in tqdm(val_loader, desc="Validation"):
                        inputs, labels = batch_data["image"].to(device), batch_data["label"].to(device)
                        
                        # 将标签值从 [0, 255] 映射到 [0, 1]
                        if num_classes <= 2:
                            labels = labels / 255.0
                        
                        outputs = model(inputs)
                        loss = loss_function(outputs, labels)
                        val_loss += loss.item()
                        
                        # 处理预测结果
                        if num_classes <= 2:
                            outputs_sigmoid = torch.sigmoid(outputs)
                            outputs_binary = (outputs_sigmoid > 0.5).float()
                            dice_metric(y_pred=outputs_binary, y=labels)
                        else:
                            outputs_softmax = torch.softmax(outputs, dim=1)
                            outputs_argmax = torch.argmax(outputs_softmax, dim=1, keepdim=True)
                            dice_metric(y_pred=outputs_argmax, y=labels)
                
                val_loss = val_loss / len(val_loader)
                
                # 计算平均Dice分数
                metric_score = dice_metric.aggregate().item()
                dice_metric.reset()
                
                # 记录当前epoch指标
                current_metrics = {
                    'dice': float(metric_score),
                    'iou': float(metric_score / (2 - metric_score))  # Dice转IoU公式
                }
                
                # 记录训练历史
                train_history['train_loss'].append(float(train_loss))
                train_history['val_loss'].append(float(val_loss))
                train_history['metrics'].append(current_metrics)
                
                logger.info(f"验证损失: {val_loss:.4f}, Dice系数: {metric_score:.4f}")
                
                # 保存最佳模型
                if metric_score > best_dice:
                    best_dice = metric_score
                    best_metrics = current_metrics
                    
                    # 保存模型权重
                    model_path = os.path.join(model_dir, "best_model.pth")
                    torch.save({
                        'epoch': epoch,
                        'model_state_dict': model.state_dict(),
                        'optimizer_state_dict': optimizer.state_dict(),
                        'best_dice': best_dice
                    }, model_path)
                    
                    logger.info(f"保存最佳模型，Dice系数: {best_dice:.4f}")
            
            except Exception as e:
                logger.error(f"训练过程中出错: {str(e)}")
                logger.error(traceback.format_exc())
                # 继续训练其他轮次
        
        # 训练完成，上传最佳模型
        final_model_path = os.path.join(model_dir, "best_model.pth")
        
        if not os.path.exists(final_model_path):
            error_msg = "训练失败，未生成有效模型"
            logger.error(error_msg)
            model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
            os.remove(status_file)  # 删除状态文件
            return
        
        # 上传模型文件到模型专用存储桶
        try:
            # 上传文件到模型专用存储桶
            upload_result = file_client.upload("model", final_model_path)
            logger.info(f"模型上传成功: {upload_result['url']}")
        except Exception as e:
            error_msg = f"上传模型失败: {str(e)}"
            logger.error(error_msg)
            model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
            os.remove(status_file)  # 删除状态文件
            return
        
        # 训练完成，更新模型状态
        train_metrics = {
            'best_metrics': best_metrics,
            'history': {
                'train_loss': train_history['train_loss'],
                'val_loss': train_history['val_loss'],
                'metrics': [
                    {k: safe_float_convert(v) for k, v in epoch_metrics.items()}
                    for epoch_metrics in train_history['metrics']
                ]
            }
        }
        
        # 转换为JSON字符串
        train_metrics_json = json.dumps(train_metrics)
        
        # 更新模型状态
        model_client.update_model_status(
            model_id=model_id,
            status=StatusConstant.MODEL_STATUS_TRAINED_SUCCESS,
            error_msg="",
            train_metrics=train_metrics_json,
            model_bucket=upload_result['bucket'],
            model_object_key=upload_result['objectKey']
        )
        
        # 删除训练状态文件，表示训练正常结束
        if os.path.exists(status_file):
            os.remove(status_file)
            
        logger.info(f"模型 {model_id} 训练完成并上传成功")
    
    except Exception as e:
        error_msg = f"模型训练失败: {str(e)}"
        logger.error(error_msg)
        logger.error(traceback.format_exc())
        model_client.update_model_status(model_id, StatusConstant.MODEL_STATUS_TRAINED_FAILED, error_msg)
        os.remove(status_file)  # 删除状态文件
    finally:
        # 确保状态文件被删除
        try:
            if os.path.exists(status_file):
                os.remove(status_file)
        except Exception as e:
            logger.error(f"删除训练状态文件失败: {str(e)}")

        # 删除模型文件
        model_path = os.path.join(model_dir, "best_model.pth")
        if os.path.exists(model_path):
            os.remove(model_path)
        # 删除数据集文件和解压后的文件
        dataset_path = os.path.join(dataset_dir, "dataset.zip")
        if os.path.exists(dataset_path):
            os.remove(dataset_path)
        # 删除解压后的文件夹
        if os.path.exists(dataset_dir):
            shutil.rmtree(dataset_dir)
            
        # 清理训练线程记录
        if model_id in training_threads:
            del training_threads[model_id]

# 修改路由，与 Java 后端接口一致
@training_bp.route('/train', methods=['POST'])
def train_model():
    """
    开始训练模型
    
    请求体:
        {
            "id": 模型ID,
            "hyperparams": 超参数对象或JSON字符串,
            "datasetBucket": "数据集存储桶",
            "datasetObjectKey": "数据集对象键"
        }
        
    响应:
        {
            "code": 状态码,
            "msg": "提示信息",
            "data": {
                "status": "训练状态" 
            }
        }
    """
    try:
        # 获取请求数据
        data = request.json
        if not data:
            return error("请求参数不能为空", code=400)
        
        # 获取模型ID
        model_id = data.get('id')
        if not model_id:
            return error("模型ID不能为空", code=400)
            
        # 检查模型是否已经在训练
        if model_id in training_threads:
            return error("该模型已经在训练中，请勿重复提交", code=400)
        
        # 获取训练超参数
        hyperparams = data.get('hyperparams', {})

        # 确保hyperparams不为None
        if hyperparams is None:
            logger.warning(f"训练超参数为None，使用默认超参数")
            hyperparams = {}

        # 如果是字符串，尝试解析为JSON
        if isinstance(hyperparams, str):
            try:
                import json
                hyperparams = json.loads(hyperparams)
            except Exception as e:
                logger.error(f"解析训练超参数失败: {str(e)}")
                return error(f"解析训练超参数失败: {str(e)}")
                
        # 获取数据集信息
        dataset_bucket = data.get('datasetBucket')
        dataset_object_key = data.get('datasetObjectKey')
        
        if not dataset_bucket or not dataset_object_key:
            return error("数据集信息不完整", code=400)
        
        # 从超参数中提取需要的参数，使用默认值防止缺失
        input_size = int(hyperparams.get('inputSize', 256))
        in_channels = int(hyperparams.get('inChannels', 3))
        num_classes = int(hyperparams.get('numClasses', 2))
        epochs = int(hyperparams.get('epochs', 50))
        batch_size = int(hyperparams.get('batchSize', 8))
        learning_rate = float(hyperparams.get('learningRate', 1e-4))
        aug_level = str(hyperparams.get('augLevel', 'medium'))
        
        logger.info(f"开始训练模型 {model_id}, 训练参数: input_size={input_size}, in_channels={in_channels}, num_classes={num_classes}, " +
                   f"epochs={epochs}, batch_size={batch_size}, learning_rate={learning_rate}, aug_level={aug_level}")
        
        # 创建训练线程
        thread = threading.Thread(
            target=train_model_thread,
            args=(
                model_id, dataset_bucket, dataset_object_key, 
                input_size, in_channels, num_classes,
                epochs, batch_size, learning_rate, aug_level
            )
        )
        
        # 启动线程
        thread.daemon = True
        thread.start()
        
        # 保存线程引用
        training_threads[model_id] = thread
        
        return success({"status": "训练已启动"}, msg="模型训练任务已启动")
    
    except Exception as e:
        logger.error(f"启动训练失败: {str(e)}")
        logger.error(traceback.format_exc())
        return error(f"启动训练失败: {str(e)}")
    

@training_bp.route('/cancel/<int:model_id>', methods=['POST'])
def cancel_training(model_id: int):
    """
    取消模型训练（仅限于正在训练中的模型）
    
    Args:
        model_id: 模型ID
        
    响应:
        {
            "code": 状态码,
            "msg": "提示信息",
            "data": {
                "status": "取消状态"
            }
        }
    """
    # 注意：由于Python线程无法强制终止，这里只是将模型状态标记为取消
    # 实际上训练线程仍会继续运行，只是结果不会被保存
    try:
        # 检查模型是否在训练中
        if model_id not in training_threads:
            return error("该模型当前不在训练中", code=400)
        
        # 更新模型状态
        model_client.update_model_status(
            model_id=model_id,
            status=StatusConstant.MODEL_STATUS_TRAINED_FAILED,
            error_msg="训练已手动取消"
        )
        
        # 删除训练状态文件
        status_file = os.path.join(TEMP_DIR, f"training_status_{model_id}.lock")
        if os.path.exists(status_file):
            try:
                os.remove(status_file)
            except Exception as e:
                logger.error(f"删除训练状态文件失败: {str(e)}")
        
        # 从训练线程队列中移除
        # 注意：这并不会真正终止线程，只是从队列中移除引用
        del training_threads[model_id]
        
        return success({"status": "训练已取消"}, msg="模型训练已取消")
    
    except Exception as e:
        logger.error(f"取消训练失败: {str(e)}")
        logger.error(traceback.format_exc())
        return error(f"取消训练失败: {str(e)}")