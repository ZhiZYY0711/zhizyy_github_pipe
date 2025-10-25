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
        # 从数据库获取有效的area ID和pipe ID
        area_ids = self._get_valid_area_ids()
        pipe_ids = self._get_valid_pipe_ids()
        
        # 选择area_id
        area_id = random.choice(area_ids) if area_ids else 1
        
        # 选择pipeline_id（不能为空）
        if not pipe_ids:
            # 如果没有管道数据，使用默认值
            pipeline_id = 1
        else:
            pipeline_id = random.choice(pipe_ids)
        
        # 获取有效的检修员ID
        repairman_ids = self._get_valid_repairman_ids()
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
    
    def _get_valid_area_ids(self):
        """获取数据库中有效的area ID列表"""
        try:
            from database import DatabaseManager
            db_manager = DatabaseManager()
            
            query = "SELECT id FROM area LIMIT 1000"
            result = db_manager.execute_query(query)
            
            if result:
                return [row['id'] for row in result]
            else:
                return [1, 2, 3]
        except Exception as e:
            print(f"获取area ID时出错: {e}")
            return [1, 2, 3]
    
    def _get_valid_pipe_ids(self):
        """获取数据库中有效的pipe ID列表"""
        try:
            from database import DatabaseManager
            db_manager = DatabaseManager()
            
            query = "SELECT id FROM pipe LIMIT 1000"
            result = db_manager.execute_query(query)
            
            if result:
                return [row['id'] for row in result]
            else:
                return []
        except Exception as e:
            print(f"获取pipe ID时出错: {e}")
            return []
    
    def _get_valid_repairman_ids(self):
        """获取数据库中有效的repairman ID列表"""
        try:
            from database import DatabaseManager
            db_manager = DatabaseManager()
            
            query = "SELECT id FROM repairman LIMIT 1000"
            result = db_manager.execute_query(query)
            
            if result:
                return [row['id'] for row in result]
            else:
                return [1, 2, 3]  # 默认值
        except Exception as e:
            print(f"获取repairman ID时出错: {e}")
            return [1, 2, 3]  # 默认值