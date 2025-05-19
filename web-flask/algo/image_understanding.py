import os
import numpy as np
from PIL import Image
import io
import base64
import cv2
import requests
from openai import OpenAI


def enhance_image(image_bytes):
    """
    对心脏医学图像进行增强处理
    :param image_bytes: 输入图片的字节数据
    :return: 增强后的图片字节数据
    """
    try:
        # 将字节数据转换为numpy数组
        image = cv2.imdecode(np.frombuffer(image_bytes, np.uint8), cv2.IMREAD_COLOR)
        
        # 图像增强处理
        # 1. 对比度增强
        lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
        l, a, b = cv2.split(lab)
        clahe = cv2.createCLAHE(clipLimit=3.0, tileGridSize=(8, 8))
        cl = clahe.apply(l)
        enhanced_lab = cv2.merge((cl, a, b))
        enhanced = cv2.cvtColor(enhanced_lab, cv2.COLOR_LAB2BGR)
        
        # 3. 降噪处理
        denoised = cv2.fastNlMeansDenoisingColored(enhanced, None, 10, 10, 7, 21)
        
        # 将处理后的图像转换回字节数据
        _, buffer = cv2.imencode('.jpg', denoised)
        return buffer.tobytes()
        
    except Exception as e:
        print('图像增强处理失败', e)
        return image_bytes


def understand_image(image_bytes):
    """
    使用多模态大语言模型进行心脏医学图像理解
    :param image_bytes: 输入图片的字节数据
    :return: 图片描述文字
    """
    try:
        image = Image.open(io.BytesIO(image_bytes))
        
        # 将图片转换为base64编码
        buffered = io.BytesIO()
        image.save(buffered, format="JPEG")
        img_str = base64.b64encode(buffered.getvalue()).decode()
        img_base64_url = f"data:image/jpeg;base64,{img_str}"
        
        # 创建OpenAI客户端连接到阿里云API
        client = OpenAI(
            api_key="sk-849620e070e04071a69da5bc1ba4ddcb",
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
        )
        
        # 提示词
        prompt = """
            请根据以下要求描述这张医学影像中的内容：
            1.描述影像中的整体结构，包括心脏、血管、肺部等主要器官
            2.描述图像中存在的红色标注框区域
            3.描述图像中存在的异常特征
        """

        # 调用通义千问VL模型，针对心脏医学影像进行分析
        completion = client.chat.completions.create(
            model="qwen-vl-plus",  # 可按需更换模型名称
            messages=[{
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt},
                    {"type": "image_url", "image_url": {"url": img_base64_url}}
                ]
            }]
        )
        
        # 解析返回结果
        response = completion.choices[0].message.content
        return response
        
    except Exception as e:
        print('通义千问VL模型调用失败', e)
        # 如果模型调用失败，使用规则化描述
        return generate_rule_based_description(image_bytes)


def generate_rule_based_description(image_bytes):
    """
    基于心脏图像处理和规则的描述生成
    """
    # 将字节数据转换为numpy数组
    image = cv2.imdecode(np.frombuffer(image_bytes, np.uint8), cv2.IMREAD_COLOR)
    
    # 1. 分析图像特征
    # 计算图像的基本特征
    brightness = np.mean(image)
    contrast = np.std(image)
    
    # 2. 边缘检测，用于识别心脏轮廓
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 50, 150)
    edge_density = np.sum(edges > 0) / (edges.shape[0] * edges.shape[1])
    
    # 3. 心脏区域分割尝试
    _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    
    # 形态学操作
    kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
    morphed = cv2.morphologyEx(thresh, cv2.MORPH_CLOSE, kernel)
    
    # 寻找轮廓
    contours, _ = cv2.findContours(morphed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    # 4. 生成描述文本
    description = "心脏医学影像分析结果：\n"
    
    # 根据图像特征生成描述
    description += f"图像整体亮度：{brightness:.1f}，对比度：{contrast:.1f}。"
    
    if edge_density > 0.05:
        description += "\n心脏边界轮廓清晰，"
    else:
        description += "\n心脏边界轮廓不明显，"
    
    # 根据轮廓数量和大小评估心脏特征
    large_contours = [c for c in contours if cv2.contourArea(c) > image.shape[0]*image.shape[1]*0.01]
    if len(large_contours) > 0:
        description += f"\n可见{len(large_contours)}个主要腔室结构。"
        
        # 计算密度变化，可能代表不同组织
        mean_intensities = []
        for contour in large_contours:
            mask = np.zeros_like(gray)
            cv2.drawContours(mask, [contour], 0, 255, -1)
            mean_int = np.mean(gray[mask == 255])
            mean_intensities.append(mean_int)
        
        std_intensity = np.std(mean_intensities) if len(mean_intensities) > 1 else 0
        if std_intensity > 20:
            description += "\n腔室间密度差异明显，可能存在心肌异常。"
        else:
            description += "\n腔室密度分布较为均匀。"
    else:
        description += "\n未能清晰分割心脏腔室结构。"
    
    description += "\n\n建议：此分析仅供参考，请结合专业医生的诊断意见。实际心脏病变诊断需要结合临床症状、多模态影像和专业医学知识。"
    
    return description 