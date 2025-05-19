"""
配置文件
"""
import os
import sys

def get_application_root():
    """获取应用程序根目录"""
    if getattr(sys, 'frozen', False):
        # 如果是打包后的可执行文件
        return os.path.dirname(sys.executable)
    else:
        # 如果是开发环境
        return os.path.dirname(__file__)

# 服务配置
HOST = os.environ.get('FLASK_SERVICE_HOST', 'localhost')
PORT = int(os.environ.get('FLASK_SERVICE_PORT', 5000))

# 文件服务配置(已经和文件端接口对齐，禁止修改)
FILE_SERVER_HOST = os.environ.get('FILE_SERVICE_HOST', 'localhost')
FILE_SERVER_PORT = int(os.environ.get('FILE_SERVICE_PORT', 5001))
FILE_SERVER_URL = f'http://{FILE_SERVER_HOST}:{FILE_SERVER_PORT}/api/file'

# 业务服务配置(已经和web-springboot对齐，禁止修改)
SPRINGBOOT_HOST = os.environ.get('SPRINGBOOT_HOST', 'localhost')
SPRINGBOOT_PORT = int(os.environ.get('SPRINGBOOT_PORT', 8080))
SPRINGBOOT_SERVER_URL = f'http://{SPRINGBOOT_HOST}:{SPRINGBOOT_PORT}/api'

# 内部服务认证令牌(已经和web-springboot对齐，禁止修改)
INTERNAL_AUTH_TOKEN = 'web-flask-internal-token'

# 临时文件配置
# 1. 首先尝试从环境变量获取
# 2. 如果环境变量未设置，则使用应用程序目录下的temp目录
TEMP_DIR = os.environ.get('FLASK_TEMP_DIR', os.path.join(get_application_root(), 'temp'))
os.makedirs(TEMP_DIR, exist_ok=True)  # 创建临时文件目录
# 缓存模型权重路径
MODEL_WEIGHTS_CACHE_DIR = os.environ.get('MODEL_WEIGHTS_CACHE_DIR', os.path.join(get_application_root(), 'model_weights_cache'))
os.makedirs(MODEL_WEIGHTS_CACHE_DIR, exist_ok=True)  # 创建缓存模型权重目录

