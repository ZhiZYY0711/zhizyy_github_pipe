#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检修员数据生成器
"""

from typing import Dict, Any, List, Optional
import random
from datetime import datetime, timedelta
from .base_generator import BaseGenerator


class RepairmanGenerator(BaseGenerator):
    """检修员数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN', custom_config: Optional[Dict] = None):
        super().__init__(locale)
        
        # 默认配置
        self.config = {
            'age_range': (22, 60),  # 年龄范围
            'sex_weights': {0: 0.7, 1: 0.25, 2: 0.05},  # 性别权重 0男 1女 2未知
            'entry_time_range': (-5, 0),  # 入职时间范围（年）
            'phone_patterns': [
                '13{8}',  # 13开头的手机号
                '15{8}',  # 15开头的手机号
                '18{8}',  # 18开头的手机号
                '17{8}',  # 17开头的手机号
            ],
            'email_domains': ['qq.com', '163.com', 'gmail.com', 'sina.com', 'company.com'],
            'name_patterns': {
                'male': ['张', '王', '李', '赵', '刘', '陈', '杨', '黄', '周', '吴'],
                'female': ['张', '王', '李', '赵', '刘', '陈', '杨', '黄', '周', '吴']
            }
        }
        
        # 合并自定义配置
        if custom_config:
            self.config.update(custom_config)
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'repairman'
    
    def generate_name(self, sex: int) -> str:
        """
        生成姓名
        
        Args:
            sex: 性别 0男 1女 2未知
            
        Returns:
            生成的姓名
        """
        if sex == 0:  # 男性
            surnames = self.config['name_patterns']['male']
            given_names = ['伟', '强', '军', '磊', '涛', '明', '超', '勇', '杰', '华', 
                          '建', '国', '志', '峰', '龙', '飞', '鹏', '辉', '斌', '刚']
        elif sex == 1:  # 女性
            surnames = self.config['name_patterns']['female']
            given_names = ['丽', '娜', '敏', '静', '秀', '美', '芳', '燕', '红', '霞',
                          '玲', '萍', '莉', '婷', '雪', '梅', '琳', '洁', '慧', '蓉']
        else:  # 未知性别
            surnames = self.config['name_patterns']['male'] + self.config['name_patterns']['female']
            given_names = ['伟', '丽', '强', '娜', '军', '敏', '磊', '静', '涛', '秀']
        
        surname = random.choice(surnames)
        given_name = random.choice(given_names)
        
        # 有30%概率生成双字名
        if random.random() < 0.3:
            given_name += random.choice(given_names)
        
        return surname + given_name
    
    def generate_sex(self) -> int:
        """
        生成性别
        
        Returns:
            性别 0男 1女 2未知
        """
        weights = self.config['sex_weights']
        choices = list(weights.keys())
        probabilities = list(weights.values())
        return random.choices(choices, weights=probabilities)[0]
    
    def generate_age(self) -> int:
        """
        生成年龄
        
        Returns:
            年龄
        """
        min_age, max_age = self.config['age_range']
        return random.randint(min_age, max_age)
    
    def generate_phone(self) -> str:
        """
        生成手机号
        
        Returns:
            手机号码
        """
        pattern = random.choice(self.config['phone_patterns'])
        # 生成8位随机数字
        digits = ''.join([str(random.randint(0, 9)) for _ in range(8)])
        # 替换{8}为8位数字
        phone = pattern.replace('{8}', digits)
        return phone
    
    def generate_email(self, name: str) -> str:
        """
        生成邮箱地址
        
        Args:
            name: 姓名
            
        Returns:
            邮箱地址
        """
        domain = random.choice(self.config['email_domains'])
        
        # 生成用户名部分
        username_patterns = [
            name.lower(),
            name.lower() + str(random.randint(1, 999)),
            self.fake.user_name(),
            f"{name.lower()}{random.randint(1980, 2000)}",
        ]
        
        username = random.choice(username_patterns)
        return f"{username}@{domain}"
    
    def generate_entry_time(self) -> datetime:
        """
        生成入职时间
        
        Returns:
            入职时间
        """
        start_years, end_years = self.config['entry_time_range']
        start_date = datetime.now() + timedelta(days=start_years * 365)
        end_date = datetime.now() + timedelta(days=end_years * 365)
        
        return self.fake.date_time_between(start_date=start_date, end_date=end_date)
    
    def generate_area_id(self) -> int:
        """
        生成区域ID（外键）
        
        Returns:
            区域ID
        """
        area_ids = self.get_cached_foreign_keys('area')
        return random.choice(area_ids) if area_ids else 1
    
    def generate_create_time(self) -> datetime:
        """
        生成创建时间
        
        Returns:
            创建时间
        """
        return self.fake.date_time_between(start_date='-2y', end_date='now')
    
    def generate_update_time(self, create_time: datetime) -> datetime:
        """
        生成更新时间
        
        Args:
            create_time: 创建时间
            
        Returns:
            更新时间
        """
        # 更新时间应该在创建时间之后
        return self.fake.date_time_between(start_date=create_time, end_date='now')
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条检修员记录"""
        # 生成基础信息
        sex = self.generate_sex()
        name = self.generate_name(sex)
        age = self.generate_age()
        phone = self.generate_phone()
        email = self.generate_email(name)
        entry_time = self.generate_entry_time()
        area_id = self.generate_area_id()
        create_time = self.generate_create_time()
        update_time = self.generate_update_time(create_time)
        
        return {
            'name': name,
            'age': age,
            'sex': sex,
            'phone': phone,
            'email': email,
            'entry_time': entry_time,
            'area_id': area_id,
            'create_time': create_time,
            'update_time': update_time
        }
    

    
    def get_field_info(self) -> Dict[str, str]:
        """
        获取字段信息
        
        Returns:
            字段信息字典
        """
        return {
            'name': '检修员姓名，根据性别生成合适的中文姓名',
            'age': f'年龄，范围：{self.config["age_range"][0]}-{self.config["age_range"][1]}岁',
            'sex': '性别，0=男，1=女，2=未知',
            'phone': '手机号码，支持13/15/17/18开头的11位号码',
            'email': '邮箱地址，基于姓名和常用域名生成',
            'entry_time': f'入职时间，范围：{self.config["entry_time_range"][0]}年前到现在',
            'area_id': '所属区域ID，从area表中随机选择',
            'create_time': '记录创建时间',
            'update_time': '记录更新时间'
        }
    
    def get_sample_config(self) -> Dict[str, Any]:
        """
        获取配置示例
        
        Returns:
            配置示例
        """
        return {
            'age_range': [25, 55],
            'sex_weights': {0: 0.8, 1: 0.15, 2: 0.05},
            'entry_time_range': [-3, 0],
            'phone_patterns': ['13{8}', '15{8}', '18{8}'],
            'email_domains': ['company.com', 'work.cn'],
            'name_patterns': {
                'male': ['张', '王', '李', '赵'],
                'female': ['张', '王', '李', '赵']
            }
        }
    
    def validate_record(self, record: Dict[str, Any]) -> bool:
        """
        验证记录数据
        
        Args:
            record: 记录数据
            
        Returns:
            验证结果
        """
        required_fields = ['name', 'entry_time', 'area_id', 'create_time', 'update_time']
        
        # 检查必填字段
        for field in required_fields:
            if field not in record or record[field] is None:
                return False
        
        # 检查字段类型和范围
        if not isinstance(record['name'], str) or len(record['name']) > 20:
            return False
        
        if record.get('age') is not None:
            if not isinstance(record['age'], int) or not (0 <= record['age'] <= 150):
                return False
        
        if record.get('sex') is not None:
            if record['sex'] not in [0, 1, 2]:
                return False
        
        if record.get('phone') is not None:
            if not isinstance(record['phone'], str) or len(record['phone']) > 15:
                return False
        
        if record.get('email') is not None:
            if not isinstance(record['email'], str) or len(record['email']) > 100:
                return False
        
        return True