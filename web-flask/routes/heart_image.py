from flask import Blueprint, request, jsonify
import os
from utils.response import success, error
from algo.image_understanding import enhance_image, understand_image
from algo.health_advisor import health_advisor

heart_image = Blueprint('heart_image', __name__, url_prefix='/api/heart')

@heart_image.route('/enhance', methods=['POST'])
def enhance():
    """
    心脏图像增强接口
    """
    try:
        # 获取上传的文件
        if 'image' not in request.files:
            return error(msg='未上传图像文件')
            
        file = request.files['image']
        if file.filename == '':
            return error(msg='未选择图像文件')
            
        # 读取文件内容
        image_bytes = file.read()
        
        # 进行图像增强
        enhanced_bytes = enhance_image(image_bytes)
        
        # 创建临时文件夹
        temp_dir = 'temp'
        os.makedirs(temp_dir, exist_ok=True)
        
        # 保存增强后的图像到临时文件
        temp_file_path = os.path.join(temp_dir, 'enhanced_' + file.filename)
        with open(temp_file_path, 'wb') as f:
            f.write(enhanced_bytes)
        
        # 上传增强后的图像到文件对象存储服务
        from clients.file_client import file_client
        result = file_client.upload('heart', temp_file_path)
        
        # 删除临时文件
        os.remove(temp_file_path)
        
        return success(data=result)
        
    except Exception as e:
        return error(msg=f'图像增强失败: {str(e)}')

@heart_image.route('/analyze', methods=['POST'])
def analyze():
    """
    心脏图像分析接口
    """
    try:
        # 获取图像URL或文件
        if request.json and (request.json.get('imageUrl') is not None or 'bucket' in request.json):
            # 从文件对象存储服务获取图像
            from clients.file_client import file_client
            bucket = request.json.get('bucket')
            object_key = request.json.get('objectKey')
            if not bucket or not object_key:
                return error(msg='缺少必要参数')
            
            # 获取图像内容
            image_bytes = file_client.get(bucket, object_key)
        elif 'image' in request.files:
            # 直接上传图像文件
            file = request.files['image']
            if file.filename == '':
                return error(msg='未选择图像文件')
                
            # 读取文件内容
            image_bytes = file.read()
        else:
            return error(msg='未提供图像')
        
        # 分析图像
        analysis_result = understand_image(image_bytes)
        
        return success(data={'analysis': analysis_result})
        
    except Exception as e:
        return error(msg=f'图像分析失败: {str(e)}')

@heart_image.route('/health-advice', methods=['POST'])
def health_advice():
    """
    根据图像分析结果生成健康建议
    """
    try:
        # 获取分析结果
        if not request.json or 'analysis' not in request.json:
            return error(msg='缺少分析结果')
            
        analysis = request.json.get('analysis')
        if not analysis or not isinstance(analysis, str):
            return error(msg='分析结果格式不正确')
        
        # 生成健康建议
        advice = health_advisor.generate_advice(analysis)
        
        return success(data={'advice': advice})
        
    except Exception as e:
        return error(msg=f'生成健康建议失败: {str(e)}') 