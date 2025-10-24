#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
传感器数据生成器
"""

from typing import Dict, Any
import random
from base_generator import BaseGenerator


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
        
        # 选择pipeline_id（可能为空）
        pipeline_id = random.choice(pipe_ids) if pipe_ids and random.random() > 0.3 else None
        
        # 生成位置坐标
        latitude = round(random.uniform(20.0, 50.0), 6)
        longitude = round(random.uniform(80.0, 130.0), 6)
        position = f"{latitude},{longitude}"
        
        # 生成检修时间（可能为空）
        last_overhaul_time = self.fake.date_time_between(start_date='-1y', end_date='now') if random.random() > 0.4 else None
        
        return {
            'area_id': area_id,
            'pipeline_id': pipeline_id,
            'repairman_id': None,  # 暂时设为空，因为还没有repairman数据
            'status': random.choice(self.sensor_status),
            'type': random.choice(self.sensor_types),
            'last_overhaul_time': last_overhaul_time,
            'position': position,
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
                return [row[0] for row in result]
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
                return [row[0] for row in result]
            else:
                return []
        except Exception as e:
            print(f"获取pipe ID时出错: {e}")
            return []