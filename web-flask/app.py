"""
算法服务入口
"""
import os
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging

# 导入配置和响应工具
from config import TEMP_DIR
from utils.response import success, error

# 导入路由模块
from routes.health import health_bp
from routes.training_routes import training_bp
from routes.dataset_routes import dataset_bp
from routes.prediction_routes import prediction_bp
from routes.heart_image import heart_image

# 创建Flask应用
app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 创建临时目录 - 使用配置中的TEMP_DIR
os.makedirs(TEMP_DIR, exist_ok=True)

# 注册路由蓝图
app.register_blueprint(health_bp, url_prefix='/api/health')
app.register_blueprint(dataset_bp, url_prefix='/api/dataset')
app.register_blueprint(training_bp, url_prefix='/api/model')
app.register_blueprint(prediction_bp, url_prefix='/api/prediction')
app.register_blueprint(heart_image)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True) 