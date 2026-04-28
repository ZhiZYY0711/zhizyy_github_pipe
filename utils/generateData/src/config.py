# -*- coding: utf-8 -*-
"""
配置管理模块
"""

import os
import yaml
from typing import Dict, Any
from dotenv import load_dotenv
from pydantic import BaseModel, Field
from loguru import logger


class DatabaseConfig(BaseModel):
    """数据库配置"""
    host: str = Field(default="localhost", description="数据库主机")
    port: int = Field(default=3306, description="数据库端口")
    user: str = Field(default="root", description="数据库用户名")
    password: str = Field(default="root", description="数据库密码")
    database: str = Field(default="pipeline_management_system", description="数据库名")
    charset: str = Field(default="utf8mb4", description="字符集")


class PoolConfig(BaseModel):
    """连接池配置"""
    pool_size: int = Field(default=10, description="连接池大小")
    max_overflow: int = Field(default=20, description="最大溢出连接数")
    pool_timeout: int = Field(default=30, description="连接超时时间")
    pool_recycle: int = Field(default=3600, description="连接回收时间")
    pool_pre_ping: bool = Field(default=True, description="连接前ping测试")


class LoggingConfig(BaseModel):
    """日志配置"""
    level: str = Field(default="INFO", description="日志级别")
    file: str = Field(default="logs/app.log", description="日志文件")
    format: str = Field(
        default="{time:YYYY-MM-DD HH:mm:ss} | {level} | {name}:{function}:{line} | {message}",
        description="日志格式"
    )
    rotation: str = Field(default="1 day", description="日志轮转")
    retention: str = Field(default="30 days", description="日志保留时间")


class AppConfig(BaseModel):
    """应用配置"""
    batch_size: int = Field(default=1000, description="批处理大小")
    max_records: int = Field(default=10000, description="最大记录数")
    timezone: str = Field(default="Asia/Shanghai", description="时区")


class Config:
    """配置管理器"""
    
    def __init__(self, config_file: str = "config/database.yaml"):
        self.config_file = config_file
        self._config_data = {}
        self.load_config()
    
    def load_config(self):
        """加载配置"""
        # 加载环境变量
        load_dotenv()
        
        # 加载YAML配置文件
        if os.path.exists(self.config_file):
            with open(self.config_file, 'r', encoding='utf-8') as f:
                self._config_data = yaml.safe_load(f)
        else:
            logger.warning(f"配置文件 {self.config_file} 不存在，使用默认配置")
            self._config_data = {}
    
    def _get_env_value(self, key: str, default: Any = None) -> Any:
        """获取环境变量值，支持默认值"""
        if isinstance(key, str) and key.startswith("${") and key.endswith("}"):
            # 解析 ${VAR_NAME:default_value} 格式
            var_part = key[2:-1]  # 去掉 ${ 和 }
            if ":" in var_part:
                var_name, default_val = var_part.split(":", 1)
                return os.getenv(var_name, default_val)
            else:
                return os.getenv(var_part, default)
        return key
    
    def _resolve_config_values(self, config_dict: Dict[str, Any]) -> Dict[str, Any]:
        """递归解析配置中的环境变量"""
        resolved = {}
        for key, value in config_dict.items():
            if isinstance(value, dict):
                resolved[key] = self._resolve_config_values(value)
            elif isinstance(value, str):
                resolved[key] = self._get_env_value(value, value)
            else:
                resolved[key] = value
        return resolved
    
    @property
    def database(self) -> DatabaseConfig:
        """获取数据库配置"""
        db_config = self._config_data.get("database", {}).get("primary", {})
        resolved_config = self._resolve_config_values(db_config)
        return DatabaseConfig(**resolved_config)
    
    @property
    def pool(self) -> PoolConfig:
        """获取连接池配置"""
        pool_config = self._config_data.get("database", {}).get("pool", {})
        resolved_config = self._resolve_config_values(pool_config)
        return PoolConfig(**resolved_config)
    
    @property
    def logging(self) -> LoggingConfig:
        """获取日志配置"""
        log_config = self._config_data.get("logging", {})
        resolved_config = self._resolve_config_values(log_config)
        return LoggingConfig(**resolved_config)
    
    @property
    def app(self) -> AppConfig:
        """获取应用配置"""
        app_config = self._config_data.get("app", {})
        resolved_config = self._resolve_config_values(app_config)
        return AppConfig(**resolved_config)


# 全局配置实例
config = Config()