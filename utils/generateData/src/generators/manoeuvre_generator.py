#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
演习数据生成器
"""

from typing import Dict, Any, List, Optional
import random
from datetime import datetime, timedelta
from .base_generator import BaseGenerator


class ManoeuvreGenerator(BaseGenerator):
    """演习数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        
        # 演练状态 0 成功 1 失败 2 进行中
        self.statuses = [0, 1, 2]
        self.status_weights = [0.7, 0.15, 0.15]  # 大部分演练成功，少数失败或进行中
        
        # 演练类型 0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训
        self.types = [0, 1, 2, 3]
        self.type_weights = [0.3, 0.25, 0.15, 0.3]  # 日常作训和状态模拟较多
        
        # 演练类型名称映射
        self.type_names = {
            0: "状态模拟",
            1: "事故模拟", 
            2: "紧急事故",
            3: "日常作训"
        }
        
        # 演练状态名称映射
        self.status_names = {
            0: "成功",
            1: "失败",
            2: "进行中"
        }
        
        # 演练描述模板
        self.detail_templates = {
            0: [  # 状态模拟
                "模拟管道正常运行状态下的各项参数监控和数据采集演练",
                "模拟管道压力波动情况下的系统响应和处理流程演练",
                "模拟传感器数据异常时的状态识别和报警机制演练",
                "模拟管道流量变化时的自动调节系统演练",
                "模拟设备正常维护期间的状态切换演练",
                "模拟管道温度监控系统的数据采集和分析演练",
                "模拟管道腐蚀监测系统的预警机制演练",
                "模拟管道振动监测系统的状态评估演练"
            ],
            1: [  # 事故模拟
                "模拟管道轻微泄漏事故的应急响应和处置流程演练",
                "模拟传感器故障导致的监测盲区应急处理演练",
                "模拟管道压力异常升高的紧急处置演练",
                "模拟管道阀门故障的应急修复演练",
                "模拟管道腐蚀穿孔的应急封堵演练",
                "模拟管道支架损坏的应急加固演练",
                "模拟管道接头松动的紧急处理演练",
                "模拟管道保温层破损的应急修复演练"
            ],
            2: [  # 紧急事故
                "模拟管道重大泄漏事故的紧急疏散和应急处置演练",
                "模拟管道爆炸事故的应急救援和现场处置演练",
                "模拟管道火灾事故的灭火救援和人员疏散演练",
                "模拟管道中毒事故的应急救护和现场封锁演练",
                "模拟管道坍塌事故的应急抢险和人员救援演练",
                "模拟管道环境污染事故的应急处置和污染控制演练",
                "模拟管道恐怖袭击的应急响应和安全防护演练",
                "模拟管道自然灾害损坏的应急抢修和恢复演练"
            ],
            3: [  # 日常作训
                "日常管道巡检作业流程和安全操作规程培训演练",
                "日常设备维护保养操作技能和安全防护演练",
                "日常数据采集和分析处理流程标准化演练",
                "日常应急设备检查和使用方法培训演练",
                "日常安全防护用品使用和维护保养演练",
                "日常通信设备操作和应急联络流程演练",
                "日常工具设备使用和安全操作规程演练",
                "日常作业现场安全检查和隐患排查演练"
            ]
        }
        
        # 演练持续时间范围（小时）
        self.duration_ranges = {
            0: (1, 4),    # 状态模拟：1-4小时
            1: (2, 8),    # 事故模拟：2-8小时  
            2: (4, 24),   # 紧急事故：4-24小时
            3: (0.5, 2)   # 日常作训：0.5-2小时
        }
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'manoeuvre'
    
    def _generate_time_sequence(self, manoeuvre_type: int, status: int) -> Dict[str, Any]:
        """
        生成演练时间序列
        
        Args:
            manoeuvre_type: 演练类型
            status: 演练状态
            
        Returns:
            包含开始时间和结束时间的字典
        """
        # 生成开始时间（过去30天内的随机时间）
        days_ago = random.randint(0, 30)
        hours_offset = random.randint(0, 23)
        minutes_offset = random.randint(0, 59)
        
        start_time = datetime.now() - timedelta(
            days=days_ago,
            hours=hours_offset,
            minutes=minutes_offset
        )
        
        # 根据演练类型确定持续时间
        min_hours, max_hours = self.duration_ranges[manoeuvre_type]
        duration_hours = random.uniform(min_hours, max_hours)
        
        # 如果状态是进行中(2)，则没有结束时间
        if status == 2:
            # 进行中的演练，开始时间应该比较近
            start_time = datetime.now() - timedelta(
                hours=random.uniform(0.1, duration_hours * 0.8)
            )
            end_time = None
        else:
            # 已完成的演练，有结束时间
            end_time = start_time + timedelta(hours=duration_hours)
            
            # 如果是失败状态，可能提前结束
            if status == 1:
                actual_duration = random.uniform(min_hours * 0.3, duration_hours * 0.8)
                end_time = start_time + timedelta(hours=actual_duration)
        
        return {
            'start_time': start_time,
            'end_time': end_time
        }
    
    def _generate_details(self, manoeuvre_type: int, status: int, area_info: str = None) -> str:
        """
        生成演练详细描述
        
        Args:
            manoeuvre_type: 演练类型
            status: 演练状态
            area_info: 区域信息
            
        Returns:
            演练详细描述
        """
        # 选择基础描述模板
        base_detail = random.choice(self.detail_templates[manoeuvre_type])
        
        # 添加区域信息
        if area_info:
            base_detail = f"在{area_info}区域进行{base_detail}"
        
        # 根据状态添加结果描述
        if status == 0:  # 成功
            success_suffixes = [
                "，演练过程顺利，达到预期目标。",
                "，参演人员配合默契，演练效果良好。",
                "，各项流程执行到位，演练圆满成功。",
                "，应急响应及时有效，演练取得成功。",
                "，操作规范标准，演练顺利完成。"
            ]
            base_detail += random.choice(success_suffixes)
        elif status == 1:  # 失败
            failure_suffixes = [
                "，演练过程中发现流程缺陷，需要改进。",
                "，部分环节执行不到位，演练效果不佳。",
                "，应急响应存在延迟，需要加强训练。",
                "，设备故障影响演练进度，未达到预期目标。",
                "，人员配合不够默契，演练存在问题。"
            ]
            base_detail += random.choice(failure_suffixes)
        else:  # 进行中
            ongoing_suffixes = [
                "，演练正在按计划进行中。",
                "，各项准备工作已就绪，演练进行中。",
                "，参演人员已到位，演练正在实施。",
                "，演练各环节正在有序推进。",
                "，演练进度正常，预计按时完成。"
            ]
            base_detail += random.choice(ongoing_suffixes)
        
        return base_detail
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条演习记录"""
        
        # 获取区域ID（外键）
        area_ids = self.get_cached_foreign_keys('area', limit=1000)
        if not area_ids:
            raise ValueError("无法获取area表的外键数据，请确保area表中有数据")
        
        area_id = random.choice(area_ids)
        
        # 生成演练类型和状态
        manoeuvre_type = random.choices(self.types, weights=self.type_weights)[0]
        status = random.choices(self.statuses, weights=self.status_weights)[0]
        
        # 生成时间序列
        time_info = self._generate_time_sequence(manoeuvre_type, status)
        
        # 生成区域信息（用于描述）
        area_info = f"第{area_id}号"
        
        # 生成详细描述
        details = self._generate_details(manoeuvre_type, status, area_info)
        
        # 生成创建和更新时间
        create_time = time_info['start_time'] - timedelta(
            hours=random.uniform(0.1, 24)  # 创建时间在开始时间前0.1-24小时
        )
        
        # 更新时间
        if time_info['end_time']:
            # 已结束的演练，更新时间在结束时间附近
            update_time = time_info['end_time'] + timedelta(
                minutes=random.randint(1, 60)
            )
        else:
            # 进行中的演练，更新时间比较近
            update_time = datetime.now() - timedelta(
                minutes=random.randint(1, 30)
            )
        
        record = {
            'area_id': area_id,
            'start_time': time_info['start_time'],
            'end_time': time_info['end_time'],
            'status': status,
            'type': manoeuvre_type,
            'details': details,
            'create_time': create_time,
            'update_time': update_time
        }
        
        return record
    
    def generate_realistic_batch(self, count: int, area_id: Optional[int] = None) -> List[Dict[str, Any]]:
        """
        生成更真实的批量演习数据
        
        Args:
            count: 生成数量
            area_id: 指定区域ID，如果为None则随机选择
            
        Returns:
            演习记录列表
        """
        records = []
        
        # 如果指定了区域ID，验证其有效性
        if area_id:
            area_ids = self.get_cached_foreign_keys('area', limit=1000)
            if area_id not in area_ids:
                raise ValueError(f"指定的area_id {area_id} 不存在")
        
        for _ in range(count):
            if area_id:
                # 临时设置area_id
                original_method = self.generate_record
                def generate_with_area():
                    record = original_method()
                    record['area_id'] = area_id
                    return record
                record = generate_with_area()
            else:
                record = self.generate_record()
            
            records.append(record)
        
        return records