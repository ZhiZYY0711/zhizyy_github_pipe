# -*- coding: utf-8 -*-
"""
检修员登录表数据生成器
"""

from typing import Dict, Any, Optional, List
from datetime import datetime, timedelta
import hashlib
import random
from .base_generator import BaseGenerator
from loguru import logger


class RepairmanRegistrationGenerator(BaseGenerator):
    """检修员登录表数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN', custom_config: Optional[Dict[str, Any]] = None):
        """
        初始化检修员登录表生成器
        
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
        self.username_prefix = self.username_config.get('prefix', 'repair')
        self.username_suffix_length = self.username_config.get('suffix_length', 4)
        self.username_patterns = self.username_config.get('patterns', [])
        
        # 密码配置
        self.password_config = self.custom_config.get('password', {})
        self.default_password = self.password_config.get('default', 'password123')
        self.password_patterns = self.password_config.get('patterns', [])
        
        # 时间配置
        self.time_config = self.custom_config.get('time', {})
        self.login_time_range_days = self.time_config.get('login_time_range_days', 30)
        self.password_update_range_days = self.time_config.get('password_update_range_days', 90)
        
        # 数据量配置
        self.data_config = self.custom_config.get('data', {})
        self.default_count = self.data_config.get('default_count', 100)
        
    def get_table_name(self) -> str:
        """获取表名"""
        return 'repairman_registration'
    
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
            # 默认生成规则
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
            
        # 生成MD5哈希（实际项目中建议使用更安全的哈希算法如bcrypt）
        return hashlib.md5(password.encode('utf-8')).hexdigest()
    
    def _generate_login_time(self) -> Optional[datetime]:
        """生成上次登录时间"""
        if random.random() < 0.1:  # 10%的概率没有登录记录
            return None
            
        # 在指定天数范围内随机生成登录时间
        days_ago = random.randint(0, self.login_time_range_days)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        
        login_time = datetime.now() - timedelta(
            days=days_ago, 
            hours=hours_ago, 
            minutes=minutes_ago
        )
        return login_time
    
    def _generate_password_update_time(self) -> datetime:
        """生成密码更新时间"""
        # 在指定天数范围内随机生成密码更新时间
        days_ago = random.randint(1, self.password_update_range_days)
        hours_ago = random.randint(0, 23)
        minutes_ago = random.randint(0, 59)
        
        update_time = datetime.now() - timedelta(
            days=days_ago,
            hours=hours_ago,
            minutes=minutes_ago
        )
        return update_time
    
    def generate_record(self) -> Dict[str, Any]:
        """
        生成单条检修员登录记录
        
        Returns:
            包含检修员登录信息的字典
        """
        try:
            # 生成密码更新时间（必须在创建时间之前）
            password_updated_at = self._generate_password_update_time()
            
            # 生成上次登录时间（可能为空，如果有值则在密码更新时间之后）
            last_login_time = self._generate_login_time()
            if last_login_time and last_login_time < password_updated_at:
                last_login_time = password_updated_at + timedelta(
                    hours=random.randint(1, 24)
                )
            
            # 创建时间在密码更新时间之前
            create_time = password_updated_at - timedelta(
                days=random.randint(1, 30),
                hours=random.randint(0, 23),
                minutes=random.randint(0, 59)
            )
            
            # 更新时间等于最后一次操作时间
            if last_login_time:
                update_time = max(last_login_time, password_updated_at)
            else:
                update_time = password_updated_at
            
            record = {
                'username': self._generate_username(),
                'password': self._generate_password_hash(),
                'last_login_time': last_login_time,
                'password_updated_at': password_updated_at,
                'create_time': create_time,
                'update_time': update_time
            }
            
            return record
            
        except Exception as e:
            logger.error(f"生成检修员登录记录时出错: {e}")
            raise
    
    def generate_batch_with_unique_usernames(self, count: int) -> List[Dict[str, Any]]:
        """
        生成批量记录，确保用户名唯一
        
        Args:
            count: 生成记录数量
            
        Returns:
            记录列表
        """
        records = []
        used_usernames = set()
        attempts = 0
        max_attempts = count * 3  # 最大尝试次数，避免无限循环
        
        while len(records) < count and attempts < max_attempts:
            try:
                record = self.generate_record()
                username = record['username']
                
                if username not in used_usernames:
                    used_usernames.add(username)
                    records.append(record)
                    
                    if len(records) % 100 == 0:
                        logger.debug(f"已生成 {len(records)} 条唯一记录")
                
                attempts += 1
                
            except Exception as e:
                logger.error(f"生成第 {len(records) + 1} 条记录时出错: {e}")
                attempts += 1
                continue
        
        if len(records) < count:
            logger.warning(f"只生成了 {len(records)} 条记录，少于预期的 {count} 条")
        
        logger.info(f"批量生成完成，共生成 {len(records)} 条唯一用户名记录")
        return records
    
    def generate_and_save_unique(self, count: Optional[int] = None, 
                               if_exists: str = 'append') -> int:
        """
        生成并保存唯一用户名的记录到数据库
        
        Args:
            count: 生成记录数量，默认使用配置中的数量
            if_exists: 如果表存在的处理方式 ('append', 'replace', 'fail')
            
        Returns:
            插入的记录数
        """
        if count is None:
            count = self.default_count
            
        if count <= 0:
            logger.warning("生成数量必须大于0")
            return 0
        
        logger.info(f"开始生成 {count} 条检修员登录记录")
        
        # 分批生成，避免内存占用过大
        total_inserted = 0
        batch_size = min(self.batch_size, count)
        
        for i in range(0, count, batch_size):
            current_batch_size = min(batch_size, count - i)
            
            logger.info(f"生成第 {i + 1} 到 {i + current_batch_size} 条记录")
            records = self.generate_batch_with_unique_usernames(current_batch_size)
            
            if records:
                inserted = self.save_to_database(records, if_exists)
                total_inserted += inserted
                
                # 第一批之后都使用append模式
                if if_exists != 'append':
                    if_exists = 'append'
        
        logger.info(f"总共插入 {total_inserted} 条记录到表 {self.get_table_name()}")
        return total_inserted