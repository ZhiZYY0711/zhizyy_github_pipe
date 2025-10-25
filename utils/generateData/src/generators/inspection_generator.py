# -*- coding: utf-8 -*-
"""
巡检数据生成器
"""

from typing import Dict, Any
import random
from .base_generator import BaseGenerator


class InspectionGenerator(BaseGenerator):
    """巡检数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        
        # 巡检类型
        self.inspection_types = ['日常巡检', '定期检查', '专项检查', '应急检查']
        
        # 巡检状态
        self.statuses = ['待巡检', '巡检中', '已完成', '异常']
        
        # 巡检结果
        self.results = ['正常', '异常', '需要维修', '需要更换']
        
        # 风险等级
        self.risk_levels = ['低', '中', '高', '极高']
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'inspection'
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条监测记录"""
        # 生成监测数据
        pressure = round(random.uniform(0.1, 10.0), 2)  # 压力值
        temperature = round(random.uniform(-20, 80), 1)  # 温度
        traffic = round(random.uniform(0.1, 100.0), 2)   # 流量
        shake = round(random.uniform(0.0, 5.0), 2)       # 震动值
        
        # 数据状态：0 安全 1 良好 2 危险 3 高危
        data_status = random.choice([0, 1, 2, 3])
        
        # 实时图片路径（可选）
        realtime_picture = f"/images/sensor_{self.fake.random_int(min=1000, max=9999)}.jpg" if random.random() > 0.5 else None
        
        # 从数据库获取有效的sensor_id
        sensor_ids = self._get_valid_sensor_ids()
        sensor_id = random.choice(sensor_ids) if sensor_ids else 1
        
        return {
            'pressure': pressure,
            'temperature': temperature,
            'traffic': traffic,
            'shake': shake,
            'data_status': data_status,
            'realtime_picture': realtime_picture,
            'sensor_id': sensor_id,
            'create_time': self.fake.date_time_between(start_date='-2y', end_date='now'),
            'update_time': self.fake.date_time_between(start_date='-1y', end_date='now')
        }
    
    def _get_valid_sensor_ids(self):
        """获取数据库中有效的sensor ID列表"""
        try:
            from database import DatabaseManager
            db_manager = DatabaseManager()
            
            query = "SELECT id FROM sensor LIMIT 1000"
            result = db_manager.execute_query(query)
            
            if result:
                return [row['id'] for row in result]
            else:
                return [1, 2, 3]  # 默认值
        except Exception as e:
            print(f"获取sensor ID时出错: {e}")
            return [1, 2, 3]  # 默认值