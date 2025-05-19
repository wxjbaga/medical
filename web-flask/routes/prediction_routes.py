"""
模型预测相关路由
"""
import os
import json
import uuid
import flask
import traceback
from typing import Dict, List, Any, Optional, Union
from flask import Blueprint, request, jsonify, send_file
import logging
import io
import base64
from PIL import Image
import numpy as np
import matplotlib.pyplot as plt
import time
import shutil
import cv2
import torch

# MONAI相关导入
from monai.transforms import (
    Compose, LoadImage, EnsureChannelFirst, ScaleIntensity, 
    ToTensor, Resize
)
from monai.networks.nets import UNet
from monai.metrics import DiceMetric

from config import TEMP_DIR, MODEL_WEIGHTS_CACHE_DIR  # 导入配置中的临时目录和权重目录
from clients.file_client import file_client
from clients.model_client import model_client
from common.constants import StatusConstant  # 使用常量类
from utils.response import api_response, success, error  # 导入统一响应函数

# 创建蓝图
prediction_bp = Blueprint('prediction', __name__)

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 模型缓存，避免重复加载模型
model_cache = {}

class MonaiPredictor:
    """MONAI模型预测器"""
    def __init__(self, model_path, in_channels, num_classes, img_size):
        self.model_path = model_path
        self.in_channels = in_channels
        self.num_classes = num_classes
        self.img_size = img_size
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        
        # 创建UNet模型
        self.model = UNet(
            spatial_dims=2,
            in_channels=in_channels,
            out_channels=num_classes if num_classes > 2 else 1,
            channels=(16, 32, 64, 128, 256),
            strides=(2, 2, 2, 2),
            num_res_units=2,
        ).to(self.device)
        
        # 加载模型权重
        checkpoint = torch.load(model_path, map_location=self.device)
        if 'model_state_dict' in checkpoint:
            self.model.load_state_dict(checkpoint['model_state_dict'])
        else:
            self.model.load_state_dict(checkpoint)
        
        self.model.eval()
        
        # 数据预处理-图像
        self.transforms_image = Compose([
            LoadImage(image_only=True),
            EnsureChannelFirst(),
            Resize(spatial_size=(img_size, img_size), mode="nearest"),
            ScaleIntensity(),
            ToTensor()
        ])
        
        # 数据预处理-标签（不进行归一化）
        self.transforms_label = Compose([
            LoadImage(image_only=True),
            EnsureChannelFirst(),
            Resize(spatial_size=(img_size, img_size), mode="nearest"),
            ToTensor()
        ])
    
    def predict(self, image_path):
        """单张图像预测"""
        # 加载并处理数据
        image = self.transforms_image(image_path).unsqueeze(0).to(self.device)
        
        # 进行推理
        with torch.no_grad():
            output = self.model(image)
            
            if self.num_classes <= 2:
                # 二分类
                output_sigmoid = torch.sigmoid(output)
                # 结果二值化
                output_binary = (output_sigmoid > 0.5).float()
                output_np = output_binary.cpu().numpy().squeeze()
            else:
                # 多分类
                output_softmax = torch.softmax(output, dim=1)
                output_argmax = torch.argmax(output_softmax, dim=1)
                output_np = output_argmax.cpu().numpy().squeeze()
        
        # 从处理后的原图、预测结果中恢复数据
        image_np = (image.cpu().numpy().squeeze() * 255).astype(np.uint8)
        pred_mask_np = (output_np * 255).astype(np.uint8) if self.num_classes <= 2 else output_np.astype(np.uint8)
        
        return image_np, pred_mask_np
    
    def evaluate(self, image_path, label_path):
        """评估单张图像"""
        # 加载并处理数据
        image = self.transforms_image(image_path).unsqueeze(0).to(self.device)
        label = self.transforms_label(label_path).unsqueeze(0).to(self.device) / 255.0
        
        # 进行推理
        with torch.no_grad():
            output = self.model(image)
            
            if self.num_classes <= 2:
                # 二分类
                output_sigmoid = torch.sigmoid(output)
                # 结果二值化
                output_binary = (output_sigmoid > 0.5).float()
                
                # 计算Dice指标
                dice_metric = DiceMetric(include_background=True, reduction="mean")
                dice_metric(y_pred=output_binary, y=label)
                dice_score = dice_metric.aggregate().item()
                
                # 转换为numpy
                output_np = output_binary.cpu().numpy().squeeze()
            else:
                # 多分类
                output_softmax = torch.softmax(output, dim=1)
                output_argmax = torch.argmax(output_softmax, dim=1, keepdim=True)
                
                # 计算Dice指标
                dice_metric = DiceMetric(include_background=True, reduction="mean")
                dice_metric(y_pred=output_argmax, y=label)
                dice_score = dice_metric.aggregate().item()
                
                # 转换为numpy
                output_np = output_argmax.cpu().numpy().squeeze()
        
        # 从处理后的原图、标签、预测结果中恢复数据
        image_np = (image.cpu().numpy().squeeze() * 255).astype(np.uint8)
        label_np = (label.cpu().numpy().squeeze() * 255).astype(np.uint8)
        pred_mask_np = (output_np * 255).astype(np.uint8) if self.num_classes <= 2 else output_np.astype(np.uint8)
        
        # 计算IoU
        iou = dice_score / (2 - dice_score)  # Dice到IoU的转换公式
        
        # 计算准确率
        accuracy = np.mean((output_np == label_np.squeeze() / 255.0).astype(np.float32))
        
        metrics = {
            "dice": float(dice_score),
            "iou": float(iou),
            "accuracy": float(accuracy)
        }
        
        # 如果是二分类，计算精确率、召回率、F1
        if self.num_classes <= 2:
            tp = np.sum((output_np > 0) & (label_np.squeeze() > 0))
            fp = np.sum((output_np > 0) & (label_np.squeeze() == 0))
            fn = np.sum((output_np == 0) & (label_np.squeeze() > 0))
            
            precision = tp / (tp + fp + 1e-7)
            recall = tp / (tp + fn + 1e-7)
            f1 = 2 * precision * recall / (precision + recall + 1e-7)
            
            metrics.update({
                "precision": float(precision),
                "recall": float(recall),
                "f1": float(f1)
            })
        
        return image_np, label_np, pred_mask_np, metrics
    
    def create_overlay(self, orig_img, pred_mask, alpha=0.5):
        """创建原图与预测掩码叠加图"""
        # 将灰度图转为彩色图
        if len(orig_img.shape) < 3 or orig_img.shape[2] == 1:
            orig_color = cv2.cvtColor(orig_img, cv2.COLOR_GRAY2BGR)
        else:
            orig_color = orig_img.copy()
        
        if self.num_classes <= 2:
            # 二分类情况 - 找到预测掩码的轮廓
            if pred_mask.dtype != np.uint8:
                pred_mask = (pred_mask * 255).astype(np.uint8)
            
            contours, _ = cv2.findContours(pred_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            # 在原图上绘制轮廓
            cv2.drawContours(orig_color, contours, -1, (255, 0, 0), 2)  # 红色轮廓
        else:
            # 多分类情况 - 创建彩色掩码
            colored_mask = np.zeros((*pred_mask.shape, 3), dtype=np.uint8)
            for i in range(1, self.num_classes):  # 跳过背景
                if i == 1:
                    colored_mask[pred_mask == i] = [255, 0, 0]  # 红色
                elif i == 2:
                    colored_mask[pred_mask == i] = [0, 255, 0]  # 绿色
                elif i == 3:
                    colored_mask[pred_mask == i] = [0, 0, 255]  # 蓝色
                elif i == 4:
                    colored_mask[pred_mask == i] = [255, 255, 0]  # 黄色
            
            # 将彩色掩码与原图混合
            overlay = cv2.addWeighted(orig_color, 1-alpha, colored_mask, alpha, 0)
            return overlay
        
        return orig_color
    
    def create_triple_overlay(self, orig_img, gt_mask, pred_mask):
        """创建三重叠加图（原图+标签+预测）"""
        # 将灰度图转为彩色图
        if len(orig_img.shape) < 3 or orig_img.shape[2] == 1:
            orig_color = cv2.cvtColor(orig_img, cv2.COLOR_GRAY2BGR)
        else:
            orig_color = orig_img.copy()
        
        if self.num_classes <= 2:
            # 二分类情况 - 找到预测掩码和真实掩码的轮廓
            if gt_mask.dtype != np.uint8:
                gt_mask = (gt_mask * 255).astype(np.uint8)
            if pred_mask.dtype != np.uint8:
                pred_mask = (pred_mask * 255).astype(np.uint8)
            
            gt_contours, _ = cv2.findContours(gt_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            pred_contours, _ = cv2.findContours(pred_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            
            # 在原图上绘制轮廓
            cv2.drawContours(orig_color, gt_contours, -1, (0, 255, 0), 2)    # 真实标签用绿色
            cv2.drawContours(orig_color, pred_contours, -1, (0, 0, 255), 2)  # 预测结果用蓝色
        else:
            # 多分类情况比较复杂，这里简化处理
            # 创建一个空白遮罩，用不同颜色标记TP(黄色)、FP(红色)、FN(绿色)
            overlay = orig_color.copy()
            
            for i in range(1, self.num_classes):  # 跳过背景类
                # 真正例(TP): 预测正确的区域 - 黄色
                tp_mask = (pred_mask == i) & (gt_mask == i)
                overlay[tp_mask] = [255, 255, 0]  # 黄色
                
                # 假正例(FP): 预测为i类但实际不是的区域 - 红色
                fp_mask = (pred_mask == i) & (gt_mask != i)
                overlay[fp_mask] = [255, 0, 0]  # 红色
                
                # 假负例(FN): 实际为i类但预测错误的区域 - 绿色
                fn_mask = (pred_mask != i) & (gt_mask == i)
                overlay[fn_mask] = [0, 255, 0]  # 绿色
            
            return overlay
        
        return orig_color

def get_predictor(model_id: int) -> Optional[MonaiPredictor]:
    """
    获取模型预测器，如果模型已加载则从缓存获取，否则新建预测器
    
    Args:
        model_id: 模型ID
        
    Returns:
        predictor: 模型预测器，如果模型不可用则返回None
    """
    global model_cache
    
    # 检查缓存
    if model_id in model_cache:
        return model_cache[model_id]
    
    try:
        # 获取模型信息
        try:
            model_data = model_client.get_model_info(model_id)
            if not model_data:
                logger.error(f"获取模型信息失败，模型ID: {model_id}")
                return None
        except Exception as e:
            logger.error(f"获取模型信息失败: {str(e)}")
            return None
        
        # 检查模型状态
        model_status = model_data.get('status')
        if model_status not in [StatusConstant.MODEL_STATUS_TRAINED_SUCCESS, StatusConstant.MODEL_STATUS_PUBLISHED]:
            logger.error(f"模型状态不可用，当前状态: {model_status}")
            return None
        
        # 获取模型文件信息
        model_bucket = model_data.get('modelBucket')
        model_object_key = model_data.get('modelObjectKey')
        
        if not model_bucket or not model_object_key:
            logger.error("模型文件信息不完整")
            return None
        
        # 前置检查模型是否已经下载到权重缓存路径中
        weights_dir = os.path.join(MODEL_WEIGHTS_CACHE_DIR, str(model_id))
        weights_path = os.path.join(weights_dir, os.path.basename(model_object_key))
        
        # 如果权重缓存路径中已有模型文件，直接使用
        if os.path.exists(weights_path):
            logger.info(f"模型文件已存在于权重缓存路径: {weights_path}")
            model_path = weights_path
        else:
            # 下载模型文件到权重缓存路径
            os.makedirs(weights_dir, exist_ok=True)
            model_path = weights_path
            
            # 下载模型文件
            try:
                file_client.download(model_bucket, model_object_key, model_path)
                logger.info(f"模型文件下载成功: {model_path}")
            except Exception as e:
                logger.error(f"下载模型文件失败: {str(e)}")
                return None
        
        # 解析训练超参数获取模型配置
        train_hyperparams = {}
        if model_data.get('trainHyperparams'):
            try:
                train_hyperparams = json.loads(model_data.get('trainHyperparams'))
            except:
                logger.warning(f"无法解析训练超参数: {model_data.get('trainHyperparams')}")
        
        # 获取模型参数（优先使用训练超参数中的配置）
        in_channels = train_hyperparams.get('inChannels') or model_data.get('inChannels', 3)
        num_classes = train_hyperparams.get('numClasses') or model_data.get('numClasses', 2)
        img_size = train_hyperparams.get('inputSize') or model_data.get('inputSize', 256)
        
        # 创建预测器
        predictor = MonaiPredictor(
            model_path=model_path,
            in_channels=in_channels,
            num_classes=num_classes,
            img_size=img_size
        )
        
        # 缓存预测器
        model_cache[model_id] = predictor
        
        return predictor
    
    except Exception as e:
        logger.error(f"加载模型失败: {str(e)}")
        logger.error(traceback.format_exc())
        return None

@prediction_bp.route('/predict', methods=['POST'])
def predict_image():
    """
    图像分割预测接口
    接收待分割图片，使用选定模型进行分割，返回分割结果
    """
    try:
        # 获取请求参数
        data = request.get_json()
        model_id = data.get('modelId')
        image_bucket = data.get('imageBucket')
        image_key = data.get('imageKey')
        
        # 参数校验
        if not all([model_id, image_bucket, image_key]):
            return error('参数不完整')
        
        # 获取预测器
        predictor = get_predictor(model_id)
        if not predictor:
            return error('模型不可用或加载失败')
        
        # 创建临时目录
        temp_dir = os.path.join(TEMP_DIR, f'pred_{int(time.time())}')
        os.makedirs(temp_dir, exist_ok=True)
        
        try:
            # 下载图片
            image_path = os.path.join(temp_dir, f'image{os.path.splitext(image_key)[1]}')
            
            # 下载图片
            file_client.download(image_bucket, image_key, image_path)
            
            # 使用MONAI预测器进行预测
            orig_img, pred_mask = predictor.predict(image_path)

            # 上传预处理后的原图
            orig_img_path = os.path.join(temp_dir, 'pred_processed_orig_img.png')
            cv2.imwrite(orig_img_path, orig_img)
            orig_img_upload_result = file_client.upload('pred_processed_orig_imgs', orig_img_path)
            
            # 上传预测掩码图
            pred_mask_path = os.path.join(temp_dir, 'pred_processed_prediction.png')
            cv2.imwrite(pred_mask_path, pred_mask)
            pred_upload_result = file_client.upload('pred_processed_predictions', pred_mask_path)
            
            # 创建叠加结果图
            overlay_img = predictor.create_overlay(orig_img, pred_mask)
            
            # 保存叠加结果图片
            overlay_path = os.path.join(temp_dir, 'pred_overlay.png')
            cv2.imwrite(overlay_path, cv2.cvtColor(overlay_img, cv2.COLOR_RGB2BGR) if len(overlay_img.shape) == 3 else overlay_img)
            
            # 上传叠加结果图片
            overlay_upload_result = file_client.upload('pred_overlay', overlay_path)
            
            # 返回结果
            result = {
                'processedOrigImg': {
                    'bucket': orig_img_upload_result['bucket'],
                    'objectKey': orig_img_upload_result['objectKey'],
                    'url': orig_img_upload_result['url']
                },
                'predictionMask': {
                    'bucket': pred_upload_result['bucket'],
                    'objectKey': pred_upload_result['objectKey'],
                    'url': pred_upload_result['url']
                },
                'overlayImage': {
                    'bucket': overlay_upload_result['bucket'],
                    'objectKey': overlay_upload_result['objectKey'],
                    'url': overlay_upload_result['url']
                }
            }
            
            return success(result)
        finally:
            # 清理临时文件
            shutil.rmtree(temp_dir, ignore_errors=True)
        
    except Exception as e:
        logger.error(f"图像分割预测失败: {str(e)}")
        logger.error(traceback.format_exc())
        return error(f"图像分割预测失败: {str(e)}")

@prediction_bp.route('/evaluate', methods=['POST'])
def evaluate_model():
    """
    模型评估接口
    接收待分割图片和标签掩码图，评估模型性能并返回评估结果
    """
    try:
        # 获取请求参数
        data = request.get_json()
        model_id = data.get('modelId')
        image_bucket = data.get('imageBucket')
        image_key = data.get('imageKey')
        mask_bucket = data.get('maskBucket')
        mask_key = data.get('maskKey')
        
        # 参数校验
        if not all([model_id, image_bucket, image_key, mask_bucket, mask_key]):
            return error('参数不完整')
        
        # 获取预测器
        predictor = get_predictor(model_id)
        if not predictor:
            return error('模型不可用或加载失败')
        
        # 创建临时目录
        temp_dir = os.path.join(TEMP_DIR, f'eval_{int(time.time())}')
        os.makedirs(temp_dir, exist_ok=True)
        
        try:
            # 下载图片和掩码
            image_path = os.path.join(temp_dir, f'image{os.path.splitext(image_key)[1]}')
            mask_path = os.path.join(temp_dir, f'mask{os.path.splitext(mask_key)[1]}')
            
            # 下载图片
            file_client.download(image_bucket, image_key, image_path)
            # 下载掩码
            file_client.download(mask_bucket, mask_key, mask_path)
            
            # 使用MONAI预测器进行评估
            orig_img, gt_mask, pred_mask, metrics = predictor.evaluate(image_path, mask_path)

            # 上传预处理后的原图
            orig_img_path = os.path.join(temp_dir, 'eval_processed_orig_img.png')
            cv2.imwrite(orig_img_path, orig_img)
            orig_img_upload_result = file_client.upload('eval_processed_orig_imgs', orig_img_path)

            # 上传预处理后的标签图
            gt_mask_path = os.path.join(temp_dir, 'eval_processed_gt_mask.png')
            cv2.imwrite(gt_mask_path, gt_mask)
            gt_mask_upload_result = file_client.upload('eval_processed_masks', gt_mask_path)
            
            # 上传预测掩码图
            pred_mask_path = os.path.join(temp_dir, 'eval_processed_prediction.png')
            cv2.imwrite(pred_mask_path, pred_mask)
            pred_upload_result = file_client.upload('eval_processed_predictions', pred_mask_path)
            
            # 创建三重叠加结果图
            triple_overlay_img = predictor.create_triple_overlay(orig_img, gt_mask, pred_mask)
            
            # 保存三重叠加结果图片
            triple_overlay_path = os.path.join(temp_dir, 'eval_processed_triple_overlay.png')
            cv2.imwrite(triple_overlay_path, cv2.cvtColor(triple_overlay_img, cv2.COLOR_RGB2BGR) if len(triple_overlay_img.shape) == 3 else triple_overlay_img)
            
            # 上传三重叠加结果图片
            triple_overlay_upload_result = file_client.upload('eval_processed_triple_overlay', triple_overlay_path)
            
            # 准备类别指标
            class_metrics = {}
            
            # 返回结果
            result = {
                'metrics': {
                    'dice': metrics['dice'],
                    'iou': metrics['iou'],
                    'accuracy': metrics['accuracy'],
                    'precision': metrics.get('precision', 0.0),
                    'recall': metrics.get('recall', 0.0),
                    'f1': metrics.get('f1', 0.0),
                    'classMetrics': class_metrics
                },
                'processedOrigImg': {
                    'bucket': orig_img_upload_result['bucket'],
                    'objectKey': orig_img_upload_result['objectKey'],
                    'url': orig_img_upload_result['url']
                },
                'processedGtMask': {
                    'bucket': gt_mask_upload_result['bucket'],
                    'objectKey': gt_mask_upload_result['objectKey'],
                    'url': gt_mask_upload_result['url']
                },
                'predictionMask': {
                    'bucket': pred_upload_result['bucket'],
                    'objectKey': pred_upload_result['objectKey'],
                    'url': pred_upload_result['url']
                },
                'overlayImage': {
                    'bucket': triple_overlay_upload_result['bucket'],
                    'objectKey': triple_overlay_upload_result['objectKey'],
                    'url': triple_overlay_upload_result['url']
                }
            }
            
            return success(result)
        finally:
            # 清理临时文件
            shutil.rmtree(temp_dir, ignore_errors=True)
        
    except Exception as e:
        logger.error(f"模型评估失败: {str(e)}")
        logger.error(traceback.format_exc())
        return error(f"模型评估失败: {str(e)}")