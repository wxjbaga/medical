import json
import requests
from typing import Dict, Any, Optional

# DeepSeek-V3模型配置
MODEL_CONFIG = {
    'name': 'DeepSeek-V3',
    'api_url': 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    'api_key': 'sk-849620e070e04071a69da5bc1ba4ddcb',
    'max_tokens': 2048,
    'temperature': 0.7,
    'model': 'deepseek-v3'
}

class HealthAdvisor:
    """心脏健康建议生成器"""
    
    def __init__(self):
        """初始化健康建议生成器"""
        self.config = MODEL_CONFIG
    
    def _make_headers(self) -> Dict:
        """创建请求头"""
        return {
            'Authorization': f'Bearer {self.config["api_key"]}',
            'Content-Type': 'application/json'
        }
    
    def generate_advice(self, analysis_result: str) -> str:
        """
        根据心脏图像分析结果生成健康建议
        
        Args:
            analysis_result: 心脏图像分析结果文本
            
        Returns:
            str: 健康建议文本
        """
        try:
            # 构建提示词
            messages = [
                {"role": "system", "content": "你是一位专业的心脏健康顾问，精通心脏医学影像分析和健康建议。请根据心脏医学影像的分析结果，提供专业、实用的健康建议和生活指导。建议应该包括生活方式调整、饮食建议、运动建议、定期检查等方面，并保持专业性和可操作性。"},
                {"role": "user", "content": f"以下是一份心脏医学影像的分析结果，请根据这些信息为患者提供心脏健康建议，不超过300字：\n\n{analysis_result}\n\n请提供详细的健康建议，包括：\n1. 生活方式调整\n2. 饮食建议\n3. 适合的运动方式\n4. 后续医疗跟进建议\n5. 需要注意的警示症状"}
            ]
            
            # 构建请求数据
            data = {
                'model': self.config['model'],
                'messages': messages,
                'max_tokens': self.config['max_tokens'],
                'temperature': self.config['temperature'],
            }
            
            # 发送请求
            response = requests.post(self.config['api_url'], json=data, headers=self._make_headers())
            print(f"API响应状态码: {response.status_code}")
            
            # 简化响应处理，直接提取内容
            result = response.json()
            
            # 如果响应包含error字段，直接使用备用建议
            if 'error' in result:
                error_msg = f"API返回错误: {result['error']}"
                print(error_msg)
                return self._generate_fallback_advice(analysis_result, error_msg)
            
            # 直接提取内容，与llm_client.py保持一致
            try:
                return result['choices'][0]['message']['content']
            except (KeyError, IndexError) as e:
                print(f"提取响应内容失败: {e}, 完整响应: {json.dumps(result)}")
                return self._generate_fallback_advice(analysis_result, f"提取响应内容失败: {e}")
            
        except Exception as e:
            error_msg = f"生成健康建议失败: {str(e)}"
            print(error_msg)
            return self._generate_fallback_advice(analysis_result, error_msg)
    
    def _generate_fallback_advice(self, analysis_result: str, error_msg: str) -> str:
        """
        生成备用健康建议（当API调用失败时）
        
        Args:
            analysis_result: 心脏图像分析结果文本
            error_msg: 错误信息
            
        Returns:
            str: 备用健康建议文本
        """
        # 提取分析结果中的关键信息
        has_abnormal = "异常" in analysis_result or "问题" in analysis_result
        has_heart_chamber = "腔室" in analysis_result
        has_density = "密度" in analysis_result or "对比度" in analysis_result
        
        # 构建通用健康建议
        advice = f"""基于心脏影像分析结果，以下是健康建议参考：

### 1. 生活方式调整
- 保持规律作息，确保充足睡眠（每晚7-8小时）
- 避免长期精神紧张和过度劳累
- 戒烟限酒，减少咖啡因摄入
- 控制体重在健康范围内

### 2. 饮食建议
- 采用低盐、低脂、低糖饮食
- 增加新鲜蔬菜和水果摄入
- 选择富含Omega-3脂肪酸的食物（如深海鱼类）
- 适量补充富含钾、镁、钙的食物
- 控制总热量摄入，维持健康体重

### 3. 适合的运动方式
- 进行中等强度有氧运动，如快走、慢跑、游泳
- 每周至少150分钟的有氧运动，分散在3-5天进行
- 避免剧烈或高强度运动
- 运动前做好充分热身，运动后做好放松

### 4. 后续医疗跟进建议
- 定期测量血压，保持记录
- 每年进行一次心脏相关检查
- 遵医嘱定期复查心脏影像"""

        # 根据分析结果添加特定建议
        if has_abnormal:
            advice += "\n- 建议尽快咨询心脏专科医生进行进一步评估"
        
        if has_heart_chamber:
            advice += "\n- 关注心脏腔室功能，定期进行超声心动图检查"
        
        if has_density:
            advice += "\n- 考虑进行冠状动脉CT或核磁共振检查，评估心肌状态"
        
        advice += """

### 5. 需要注意的警示症状
- 胸痛、胸闷、心悸
- 呼吸困难，特别是夜间或躺下时加重
- 运动耐力明显下降
- 下肢水肿
- 晕厥或近晕厥
- 出现以上症状请立即就医

【注意】本建议由系统自动生成，API调用失败，仅供参考。请务必咨询专业医生获取个性化的健康建议。"""

        return advice
    
# 创建全局实例
health_advisor = HealthAdvisor() 