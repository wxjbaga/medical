"""
业务服务客户端
"""
import requests
from config import SPRINGBOOT_SERVER_URL, INTERNAL_AUTH_TOKEN
from typing import Optional, Any, Dict

class SpringbootClient:
    """业务服务客户端"""
    
    def __init__(self):
        # 后端服务地址(已经和后端联调好，禁止修改)  
        self.server_url = SPRINGBOOT_SERVER_URL
        # 内部认证token(已经和后端联调好，禁止修改)
        self.headers = {
            'X-Internal-Auth': INTERNAL_AUTH_TOKEN,
            'Content-Type': 'application/json'
        }
    
    def _handle_response(self, response: requests.Response) -> Any:
        """
        处理响应结果
        
        Args:
            response: 响应对象
            
        Returns:
            响应数据
            
        Raises:
            Exception: 请求失败时抛出异常
        """
        if response.status_code == 200:
            result = response.json()
            if result['code'] == 200:
                return result.get('data')
            raise Exception(result['msg'])
        raise Exception(f"请求失败: HTTP {response.status_code}")

    def get(self, path: str, params: Optional[Dict] = None) -> Any:
        """
        发送GET请求
        
        Args:
            path: 请求路径
            params: 查询参数
            
        Returns:
            响应数据
            
        Raises:
            Exception: 请求失败时抛出异常
        """
        try:
            url = f"{self.server_url}{path}"
            response = requests.get(url, params=params, headers=self.headers)
            return self._handle_response(response)
        except Exception as e:
            raise Exception(f"GET请求失败: {str(e)}")

    def post(self, path: str, json: Optional[Dict] = None, data: Optional[Dict] = None) -> Any:
        """
        发送POST请求
        
        Args:
            path: 请求路径
            json: JSON数据
            data: 表单数据
            
        Returns:
            响应数据
            
        Raises:
            Exception: 请求失败时抛出异常
        """
        try:
            url = f"{self.server_url}{path}"
            response = requests.post(url, json=json, data=data, headers=self.headers)
            return self._handle_response(response)
        except Exception as e:
            raise Exception(f"POST请求失败: {str(e)}")

    def put(self, path: str, json: Optional[Dict] = None) -> Any:
        """
        发送PUT请求
        
        Args:
            path: 请求路径
            json: JSON数据
            
        Returns:
            响应数据
            
        Raises:
            Exception: 请求失败时抛出异常
        """
        try:
            url = f"{self.server_url}{path}"
            response = requests.put(url, json=json, headers=self.headers)
            return self._handle_response(response)
        except Exception as e:
            raise Exception(f"PUT请求失败: {str(e)}")

    def delete(self, path: str, json: Optional[Dict] = None) -> Any:
        """
        发送DELETE请求
        
        Args:
            path: 请求路径
            json: JSON数据
            
        Returns:
            响应数据
            
        Raises:
            Exception: 请求失败时抛出异常
        """
        try:
            url = f"{self.server_url}{path}"
            response = requests.delete(url, json=json, headers=self.headers)
            return self._handle_response(response)
        except Exception as e:
            raise Exception(f"DELETE请求失败: {str(e)}")

# 创建全局实例
springboot_client = SpringbootClient() 