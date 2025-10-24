# -*- coding: utf-8 -*-
"""
区域数据生成器
"""

from typing import Dict, Any
import random
from base_generator import BaseGenerator


class AreaGenerator(BaseGenerator):
    """区域数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        
        # 区域类型
        self.area_types = ['省', '市', '县', '区', '镇', '村']
        
        # 区域级别
        self.area_levels = [1, 2, 3, 4, 5, 6]
        
        # 状态
        self.statuses = ['active', 'inactive']
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'area'
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条区域记录"""
        # 生成省份、城市、区县
        province = self.fake.province()
        city = self.fake.city()
        district = self.fake.district()
        
        return {
            'province': province,
            'city': city,
            'district': district,
            'create_time': self.fake.date_time_between(start_date='-2y', end_date='now'),
            'update_time': self.fake.date_time_between(start_date='-1y', end_date='now')
        }