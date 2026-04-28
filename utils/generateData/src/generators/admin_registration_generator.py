# -*- coding: utf-8 -*-
"""
管理员登录表数据生成器
"""

from typing import Dict, Any, Optional, List
from datetime import datetime, timedelta
import hashlib
import random
from .base_generator import BaseGenerator
from loguru import logger


class AdminRegistrationGenerator(BaseGenerator):
    """管理员登录表数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN', custom_config: Optional[Dict[str, Any]] = None):
        """
        初始化管理员登录表生成器
        
        Args:
            locale: 本地化设置，默认中文
            custom_config: 自定义配置，用于覆盖默认随机数据生成规则
        """
        super().__init__(locale)
        self.custom_config = custom_config or {}
        self._setup_config()
        
    def _setup_config(self):
        """设置配置参数"""
        # 用户名配置
        self.username_config = self.custom_config.get('username', {})
        self.username_prefix = self.username_config.get('prefix', 'admin')
        self.username_suffix_length = self.username_config.get('suffix_length', 3)
        self.username_patterns = self.username_config.get('patterns', [])
        
        # 密码配置
        self.password_config = self.custom_config.get('password', {})
        self.default_password = self.password_config.get('default', 'admin123')
        self.password_patterns = self.password_config.get('patterns', [])
        
        # 时间配置
        self.time_config = self.custom_config.get('time', {})
        self.login_time_range_days = self.time_config.get('login_time_range_days', 7)
        self.password_update_range_days = self.time_config.get('password_update_range_days', 30)
        
        # 数据量配置
        self.data_config = self.custom_config.get('data', {})
        self.default_count = self.data_config.get('default_count', 10)
        
    def get_table_name(self) -> str:
        """获取表名"""
        return 'admin_registration'
    
    def _generate_username(self) -> str:
        """生成用户名"""
        if self.username_patterns:
            # 使用自定义用户名模式
            pattern = random.choice(self.username_patterns)
            if isinstance(pattern, str):
                return pattern
            elif isinstance(pattern, dict):
                prefix = pattern.get('prefix', self.username_prefix)
                suffix_length = pattern.get('suffix_length', self.username_suffix_length)
                suffix = ''.join([str(random.randint(0, 9)) for _ in range(suffix_length)])
                return f"{prefix}{suffix}"
        else:
            # 默认生成规则：admin + 3位数字
            suffix = ''.join([str(random.randint(0, 9)) for _ in range(self.username_suffix_length)])
            return f"{self.username_prefix}{suffix}"
    
    def _generate_password_hash(self) -> str:
        """生成密码哈希值"""
        if self.password_patterns:
            # 使用自定义密码模式
            password = random.choice(self.password_patterns)
        else:
            # 使用默认密码
            password = self.default_password
            
        # 生成MD5哈希值（实际项目中建议使用更安全的哈希算法如bcrypt）
        return hashlib.md5(password.encode('utf-8')).hexdigest()
    
    def _generate_last_login_time(self) -> datetime:
        """生成上次登录时间"""
        # 在最近指定天数内随机生成登录时间
        now = datetime.now()
        days_ago = random.randint(0, self.login_time_range_days)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        
        login_time = now - timedelta(days=days_ago, hours=hours_ago, minutes=minutes_ago)
        return login_time
    
    def _generate_password_updated_at(self) -> datetime:
        """生成密码更新时间"""
        # 在最近指定天数内随机生成密码更新时间
        now = datetime.now()
        days_ago = random.randint(1, self.password_update_range_days)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        
        update_time = now - timedelta(days=days_ago, hours=hours_ago, minutes=minutes_ago)
        return update_time
    
    def _generate_create_time(self) -> datetime:
        """生成创建时间"""
        # 创建时间应该早于密码更新时间
        now = datetime.now()
        days_ago = random.randint(self.password_update_range_days + 1, self.password_update_range_days + 365)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        
        create_time = now - timedelta(days=days_ago, hours=hours_ago, minutes=minutes_ago)
        return create_time
    
    def _generate_update_time(self) -> datetime:
        """生成更新时间"""
        # 更新时间通常是最近的时间
        now = datetime.now()
        days_ago = random.randint(0, 7)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        
        update_time = now - timedelta(days=days_ago, hours=hours_ago, minutes=minutes_ago)
        return update_time
    
    def generate_record(self) -> Dict[str, Any]:
        """
        生成单条管理员登录记录
        
        Returns:
            包含管理员登录信息的字典
        """
        try:
            # 生成时间字段（确保时间逻辑合理）
            create_time = self._generate_create_time()
            password_updated_at = self._generate_password_updated_at()
            last_login_time = self._generate_last_login_time()
            update_time = self._generate_update_time()
            
            # 确保时间逻辑正确：创建时间 <= 密码更新时间 <= 登录时间 <= 更新时间
            if password_updated_at < create_time:
                password_updated_at = create_time + timedelta(days=random.randint(1, 30))
            
            if last_login_time < password_updated_at:
                last_login_time = password_updated_at + timedelta(hours=random.randint(1, 24))
            
            if update_time < last_login_time:
                update_time = last_login_time + timedelta(minutes=random.randint(1, 60))
            
            record = {
                'username': self._generate_username(),
                'password': self._generate_password_hash(),
                'last_login_time': last_login_time,
                'password_updated_at': password_updated_at,
                'create_time': create_time,
                'update_time': update_time
            }
            
            logger.debug(f"生成管理员登录记录: {record['username']}")
            return record
            
        except Exception as e:
            logger.error(f"生成管理员登录记录时出错: {e}")
            raise
    
    def get_field_info(self) -> Dict[str, str]:
        """
        获取字段信息
        
        Returns:
            字段名称和描述的映射
        """
        return {
            'id': '管理员的唯一标识（自增主键）',
            'username': '用户名（唯一，非空）',
            'password': '密码哈希值（增强安全性）',
            'last_login_time': '上次登录时间',
            'password_updated_at': '上次修改密码时间',
            'create_time': '创建时间',
            'update_time': '更新时间'
        }
    
    def get_sample_config(self) -> Dict[str, Any]:
        """
        获取示例配置
        
        Returns:
            示例配置字典
        """
        return {
            'username': {
                'prefix': 'admin',
                'suffix_length': 3,
                'patterns': ['admin001', 'admin002', 'superadmin', 'sysadmin']
            },
            'password': {
                'default': 'admin123',
                'patterns': ['admin123', 'password123', 'admin@2024']
            },
            'time': {
                'login_time_range_days': 7,
                'password_update_range_days': 30
            },
            'data': {
                'default_count': 10
            }
        }
    
    def validate_record(self, record: Dict[str, Any]) -> bool:
        """
        验证记录的有效性
        
        Args:
            record: 要验证的记录
            
        Returns:
            验证结果
        """
        try:
            # 检查必需字段
            required_fields = ['username', 'password', 'last_login_time', 
                             'password_updated_at', 'create_time', 'update_time']
            
            for field in required_fields:
                if field not in record or record[field] is None:
                    logger.error(f"缺少必需字段: {field}")
                    return False
            
            # 检查用户名长度
            if len(record['username']) > 20:
                logger.error(f"用户名长度超过限制: {record['username']}")
                return False
            
            # 检查密码哈希长度
            if len(record['password']) > 255:
                logger.error(f"密码哈希长度超过限制")
                return False
            
            # 检查时间逻辑
            create_time = record['create_time']
            password_updated_at = record['password_updated_at']
            last_login_time = record['last_login_time']
            update_time = record['update_time']
            
            if not (create_time <= password_updated_at <= last_login_time <= update_time):
                logger.warning("时间逻辑可能不合理，但不影响数据生成")
            
            return True
            
        except Exception as e:
            logger.error(f"验证记录时出错: {e}")
            return False