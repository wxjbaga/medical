"""
数据集处理工具类
"""
import os
import json
import zipfile
import tempfile
import shutil
import glob
from typing import Tuple, Dict, Any, List

# 支持的2D图像格式
SUPPORTED_2D_IMAGE_FORMATS = ['.png', '.jpg', '.jpeg', '.tif', '.tiff']

def extract_zip(zip_file_path: str, extract_dir: str) -> str:
    """
    解压ZIP文件
    
    Args:
        zip_file_path: ZIP文件路径
        extract_dir: 解压目录
        
    Returns:
        str: 数据集根目录路径（可能包含一个父文件夹）
    """
    with zipfile.ZipFile(zip_file_path, 'r') as zip_ref:
        zip_ref.extractall(extract_dir)
        
    # 检查解压后的文件结构
    # 如果有单一的根目录，返回该目录
    entries = os.listdir(extract_dir)
    if len(entries) == 1 and os.path.isdir(os.path.join(extract_dir, entries[0])):
        # 返回数据集根目录（包含一个父文件夹）
        return os.path.join(extract_dir, entries[0])
    else:
        # 直接返回解压目录
        return extract_dir

def find_dataset_root(dir_path: str) -> str:
    """
    查找数据集根目录
    
    寻找包含train和val目录的目录
    
    Args:
        dir_path: 开始寻找的目录
        
    Returns:
        str: 找到的数据集根目录
    """
    # 首先检查当前目录是否为数据集根目录
    if os.path.isdir(os.path.join(dir_path, 'train')) and os.path.isdir(os.path.join(dir_path, 'val')):
        return dir_path
    
    # 检查当前目录的直接子目录
    for item in os.listdir(dir_path):
        item_path = os.path.join(dir_path, item)
        if os.path.isdir(item_path):
            if os.path.isdir(os.path.join(item_path, 'train')) and os.path.isdir(os.path.join(item_path, 'val')):
                return item_path
    
    # 如果没有找到有效的数据集根目录，返回原始目录
    return dir_path

def validate_2d_dataset(dataset_dir: str) -> Tuple[bool, str, Dict[str, int]]:
    """
    验证2D数据集格式
    
    Args:
        dataset_dir: 数据集目录
        
    Returns:
        验证结果: (是否通过验证, 错误信息, 统计信息)
    """
    stats = {'train_count': 0, 'val_count': 0}
    
    # 检查目录结构
    train_dir = os.path.join(dataset_dir, 'train')
    val_dir = os.path.join(dataset_dir, 'val')
    
    if not os.path.isdir(train_dir):
        return False, "缺少训练集目录(train)", stats
    
    if not os.path.isdir(val_dir):
        return False, "缺少验证集目录(val)", stats
    
    # 检查训练集
    train_images_dir = os.path.join(train_dir, 'images')
    train_masks_dir = os.path.join(train_dir, 'labels')
    
    if not os.path.isdir(train_images_dir):
        return False, "缺少训练集图像目录(train/images)", stats
    
    if not os.path.isdir(train_masks_dir):
        return False, "缺少训练集掩码目录(train/labels)", stats
    
    # 检查验证集
    val_images_dir = os.path.join(val_dir, 'images')
    val_masks_dir = os.path.join(val_dir, 'labels')
    
    if not os.path.isdir(val_images_dir):
        return False, "缺少验证集图像目录(val/images)", stats
    
    if not os.path.isdir(val_masks_dir):
        return False, "缺少验证集掩码目录(val/labels)", stats
    
    # 检查训练集图像和掩码
    train_images = set()  # 使用集合避免重复
    for ext in SUPPORTED_2D_IMAGE_FORMATS:
        # 小写扩展名
        train_images.update(glob.glob(os.path.join(train_images_dir, f'*{ext}')))
        # 大写扩展名
        train_images.update(glob.glob(os.path.join(train_images_dir, f'*{ext.upper()}')))
    
    # 转换为列表
    train_images = list(train_images)
    
    if not train_images:
        return False, "训练集图像目录为空", stats
    
    stats['train_count'] = len(train_images)
    
    # 检查训练集图像和掩码是否匹配
    for image_path in train_images:
        image_filename = os.path.basename(image_path)
        mask_path = None
        
        # 查找对应的掩码文件(支持不同扩展名)
        image_name_without_ext = os.path.splitext(image_filename)[0]
        for ext in SUPPORTED_2D_IMAGE_FORMATS:
            potential_mask = os.path.join(train_masks_dir, f"{image_name_without_ext}{ext}")
            if os.path.exists(potential_mask):
                mask_path = potential_mask
                break
                
        if not mask_path:
            return False, f"训练集图像 {image_filename} 缺少对应的掩码文件", stats
    
    # 检查验证集图像和掩码
    val_images = set()  # 使用集合避免重复
    for ext in SUPPORTED_2D_IMAGE_FORMATS:
        # 小写扩展名
        val_images.update(glob.glob(os.path.join(val_images_dir, f'*{ext}')))
        # 大写扩展名
        val_images.update(glob.glob(os.path.join(val_images_dir, f'*{ext.upper()}')))
    
    # 转换为列表
    val_images = list(val_images)
    
    if not val_images:
        return False, "验证集图像目录为空", stats
    
    stats['val_count'] = len(val_images)
    
    # 检查验证集图像和掩码是否匹配
    for image_path in val_images:
        image_filename = os.path.basename(image_path)
        mask_path = None
        
        # 查找对应的掩码文件(支持不同扩展名)
        image_name_without_ext = os.path.splitext(image_filename)[0]
        for ext in SUPPORTED_2D_IMAGE_FORMATS:
            potential_mask = os.path.join(val_masks_dir, f"{image_name_without_ext}{ext}")
            if os.path.exists(potential_mask):
                mask_path = potential_mask
                break
                
        if not mask_path:
            return False, f"验证集图像 {image_filename} 缺少对应的掩码文件", stats
    
    return True, "", stats


def validate_dataset(dataset_path: str) -> Dict[str, Any]:
    """
    验证数据集
    
    Args:
        dataset_path: 数据集文件路径
        
    Returns:
        Dict: 包含验证结果的字典
        {
            'success': 是否验证成功,
            'error': 错误信息,
            'train_count': 训练样本数量,
            'val_count': 验证样本数量
        }
    """
    # 创建临时目录
    temp_dir = tempfile.mkdtemp()
    
    try:
        # 解压ZIP文件
        extracted_path = extract_zip(dataset_path, temp_dir)
        
        # 查找数据集根目录
        dataset_root = find_dataset_root(extracted_path)
        
        # 验证数据集格式
        valid, error_msg, stats = validate_2d_dataset(dataset_root)           
        
        # 返回验证结果 - 确保字段名称与后端期望的一致
        return {
            'success': valid,
            'error': error_msg,
            'train_count': stats['train_count'],
            'val_count': stats['val_count']
        }
    except Exception as e:
        return {
            'success': False,
            'error': f"验证数据集时发生错误: {str(e)}",
            'train_count': 0,
            'val_count': 0
        }
    finally:
        # 清理临时目录
        shutil.rmtree(temp_dir, ignore_errors=True) 