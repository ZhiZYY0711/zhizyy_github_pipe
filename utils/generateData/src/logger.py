# -*- coding: utf-8 -*-
"""
日志管理模块
"""

import os
import sys
from loguru import logger
from config import config


def setup_logger():
    """设置日志配置"""
    # 移除默认的日志处理器
    logger.remove()
    
    log_config = config.logging
    
    # 确保日志目录存在
    log_dir = os.path.dirname(log_config.file)
    if log_dir and not os.path.exists(log_dir):
        os.makedirs(log_dir, exist_ok=True)
    
    # 添加控制台日志处理器
    logger.add(
        sys.stdout,
        level=log_config.level,
        format=log_config.format,
        colorize=True,
        backtrace=True,
        diagnose=True
    )
    
    # 添加文件日志处理器
    logger.add(
        log_config.file,
        level=log_config.level,
        format=log_config.format,
        rotation=log_config.rotation,
        retention=log_config.retention,
        compression="zip",
        backtrace=True,
        diagnose=True,
        encoding="utf-8"
    )
    
    logger.info("日志系统初始化完成")


# 初始化日志
setup_logger()