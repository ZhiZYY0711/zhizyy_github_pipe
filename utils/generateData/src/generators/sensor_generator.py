#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
传感器数据生成器
"""

from typing import Dict, Any
import random
from .base_generator import BaseGenerator


class SensorGenerator(BaseGenerator):
    """传感器数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        
        # 传感器类型
        self.sensor_types = [0, 1, 2, 3, 4]  # 不同类型的传感器
        
        # 传感器状态
        self.sensor_status = [0, 1, 2]  # 0 正常 1 异常 2 离线
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'sensor'
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条传感器记录"""
        # 使用缓存获取有效的外键ID
        area_ids = self.get_cached_foreign_keys('area')
        pipe_ids = self.get_cached_foreign_keys('pipe')
        repairman_ids = self.get_cached_foreign_keys('repairman')
        
        # 选择area_id
        area_id = random.choice(area_ids) if area_ids else 1
        
        # 选择pipeline_id（不能为空）
        pipeline_id = random.choice(pipe_ids) if pipe_ids else 1
        
        # 选择repairman_id
        repairman_id = random.choice(repairman_ids) if repairman_ids else 1
        
        # 生成检修时间（可能为空）
        last_overhaul_time = self.fake.date_time_between(start_date='-1y', end_date='now') if random.random() > 0.4 else None
        
        return {
            'area_id': area_id,
            'pipeline_id': pipeline_id,
            'repairman_id': repairman_id,
            'status': random.choice(self.sensor_status),
            'type': random.choice(self.sensor_types),
            'last_overhaul_time': last_overhaul_time,
            'create_time': self.fake.date_time_between(start_date='-2y', end_date='now'),
            'update_time': self.fake.date_time_between(start_date='-1y', end_date='now')
        }