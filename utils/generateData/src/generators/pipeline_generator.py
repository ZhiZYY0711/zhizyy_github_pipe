# -*- coding: utf-8 -*-
"""
管道数据生成器
"""

from typing import Dict, Any
import random
from base_generator import BaseGenerator


class PipelineGenerator(BaseGenerator):
    """管道数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        
        # 管道类型
        self.pipeline_types = ['天然气管道', '石油管道', '成品油管道', '化工管道']
        
        # 管道材质
        self.materials = ['钢管', '塑料管', '复合管', '铸铁管']
        
        # 管道状态
        self.statuses = ['运行中', '维护中', '停用', '建设中']
        
        # 压力等级
        self.pressure_levels = ['低压', '中压', '高压', '超高压']
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'pipe'
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条管道记录"""
        # 从数据库获取有效的area ID列表
        area_ids = self._get_valid_area_ids()
        
        if len(area_ids) < 2:
            # 如果area表中的记录太少，使用默认值
            start_area_id = 1
            end_area_id = 2
        else:
            # 随机选择两个不同的area ID
            start_area_id = random.choice(area_ids)
            end_area_id = random.choice([aid for aid in area_ids if aid != start_area_id])
        
        # 生成管道名称（限制在10个字符以内）
        pipe_types = ['油管', '气管', '水管']
        pipe_name = f"{random.choice(pipe_types)}{self.fake.random_int(min=1, max=999)}"
        
        return {
            'start': start_area_id,
            'end': end_area_id,
            'name': pipe_name,
            'create_time': self.fake.date_time_between(start_date='-2y', end_date='now'),
            'update_time': self.fake.date_time_between(start_date='-1y', end_date='now')
        }
    
    def _get_valid_area_ids(self):
        """获取数据库中有效的area ID列表"""
        try:
            from database import DatabaseManager
            db_manager = DatabaseManager()
            
            # 查询area表中的所有ID
            query = "SELECT id FROM area LIMIT 1000"
            result = db_manager.execute_query(query)
            
            if result:
                return [row[0] for row in result]
            else:
                return [1, 2, 3]  # 默认值
        except Exception as e:
            print(f"获取area ID时出错: {e}")
            return [1, 2, 3]  # 默认值