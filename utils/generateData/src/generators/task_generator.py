#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
任务数据生成器
"""

from typing import Dict, Any, List
import random
from datetime import datetime, timedelta
from .base_generator import BaseGenerator


class TaskGenerator(BaseGenerator):
    """任务数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        
        # 任务类型
        self.task_types = [0, 1, 2, 3, 4]  # 不同类型的任务
        
        # 任务优先级 0 紧急 1 高 2 中
        self.task_priorities = [0, 1, 2]
        self.priority_weights = [0.2, 0.3, 0.5]  # 紧急任务较少，中等优先级较多
        
        # 任务状态 0 已发布 1 已接取 2已完成 3 异常
        self.task_statuses = [0, 1, 2, 3]
        self.status_weights = [0.3, 0.25, 0.4, 0.05]  # 大部分任务已完成
        
        # 任务名称模板
        self.task_name_templates = [
            "管道{pipe_section}压力异常检修",
            "传感器{sensor_id}故障维修",
            "管道{pipe_section}泄漏检测",
            "区域{area}安全巡检",
            "管道{pipe_section}定期维护",
            "传感器{sensor_id}校准任务",
            "管道{pipe_section}清洁作业",
            "区域{area}设备更换",
            "管道{pipe_section}焊接修复",
            "传感器{sensor_id}升级任务",
            "管道{pipe_section}防腐处理",
            "区域{area}应急演练",
            "管道{pipe_section}流量调节",
            "传感器{sensor_id}数据采集",
            "管道{pipe_section}温度监控"
        ]
        
        # 反馈信息模板
        self.feedback_templates = [
            "任务已按时完成，设备运行正常。",
            "发现轻微异常，已进行调整，建议后续关注。",
            "任务完成，但需要更换部分老化组件。",
            "检修过程顺利，所有指标恢复正常范围。",
            "任务完成，发现潜在安全隐患，已上报处理。",
            "设备维护完成，性能有所提升。",
            "任务执行中遇到技术难题，已联系专家协助解决。",
            "检修完成，建议增加巡检频率。",
            "任务顺利完成，设备状态良好。",
            "发现设备老化严重，建议制定更换计划。",
            "任务完成，但天气条件影响了作业进度。",
            "检修过程中发现其他相关问题，一并处理完成。",
            "任务按计划执行，质量符合标准要求。",
            "设备调试完成，运行参数已优化。",
            "任务完成，现场环境整理完毕。"
        ]
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'task'
    
    def _generate_task_name(self, area_name: str = None, pipe_name: str = None) -> str:
        """
        生成任务名称
        
        Args:
            area_name: 区域名称
            pipe_name: 管道名称
            
        Returns:
            任务名称
        """
        template = random.choice(self.task_name_templates)
        
        # 替换模板中的占位符
        task_name = template.format(
            pipe_section=pipe_name or f"P{random.randint(1, 999):03d}段",
            sensor_id=f"S{random.randint(1000, 9999)}",
            area=area_name or f"区域{random.randint(1, 99)}"
        )
        
        return task_name
    
    def _generate_time_sequence(self, status: int) -> Dict[str, Any]:
        """
        根据任务状态生成时间序列
        
        Args:
            status: 任务状态
            
        Returns:
            时间字典
        """
        # 任务发布时间（过去30天内）
        public_time = self.fake.date_time_between(start_date='-30d', end_date='now')
        
        times = {'public_time': public_time}
        
        if status >= 1:  # 已接取或更高状态
            # 响应时间（发布后1-24小时内）
            response_time = public_time + timedelta(
                hours=random.randint(1, 24),
                minutes=random.randint(0, 59)
            )
            times['response_time'] = response_time
            
            if status >= 2:  # 已完成或异常状态
                # 完成时间（响应后1-72小时内）
                accomplish_time = response_time + timedelta(
                    hours=random.randint(1, 72),
                    minutes=random.randint(0, 59)
                )
                times['accomplish_time'] = accomplish_time
        
        return times
    
    def _should_have_inspection_id(self, task_type: int, priority: int) -> bool:
        """
        判断任务是否应该关联监测记录
        
        Args:
            task_type: 任务类型
            priority: 任务优先级
            
        Returns:
            是否需要关联监测记录
        """
        # 紧急任务和高优先级任务更可能由监测异常触发
        if priority == 0:  # 紧急
            return random.random() < 0.8
        elif priority == 1:  # 高
            return random.random() < 0.6
        else:  # 中等
            return random.random() < 0.3
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条任务记录"""
        # 使用缓存获取有效的外键ID
        area_ids = self.get_cached_foreign_keys('area')
        pipe_ids = self.get_cached_foreign_keys('pipe')
        repairman_ids = self.get_cached_foreign_keys('repairman')
        inspection_ids = self.get_cached_foreign_keys('inspection')
        
        # 必须的外键ID
        area_id = random.choice(area_ids) if area_ids else 1
        pipe_id = random.choice(pipe_ids) if pipe_ids else 1
        repairman_id = random.choice(repairman_ids) if repairman_ids else 1
        
        # 生成任务基本属性
        task_type = random.choice(self.task_types)
        priority = random.choices(self.task_priorities, weights=self.priority_weights)[0]
        status = random.choices(self.task_statuses, weights=self.status_weights)[0]
        
        # 生成任务名称
        task_name = self._generate_task_name()
        
        # 生成时间序列
        time_data = self._generate_time_sequence(status)
        
        # 可选的inspection_id
        inspection_id = None
        if self._should_have_inspection_id(task_type, priority) and inspection_ids:
            inspection_id = random.choice(inspection_ids)
        
        # 生成反馈信息（已完成或异常状态才有）
        feedback_information = None
        if status in [2, 3]:  # 已完成或异常
            if random.random() < 0.8:  # 80%的概率有反馈
                feedback_information = random.choice(self.feedback_templates)
                # 有时候添加一些具体的数值信息
                if random.random() < 0.3:
                    additional_info = f" 处理时长: {random.randint(30, 480)}分钟。"
                    feedback_information += additional_info
        
        # 构建记录
        record = {
            'inspection_id': inspection_id,
            'repairman_id': repairman_id,
            'area_id': area_id,
            'pipe_id': pipe_id,
            'task_name': task_name,
            'type': task_type,
            'priority': priority,
            'status': status,
            'public_time': time_data['public_time'],
            'response_time': time_data.get('response_time'),
            'accomplish_time': time_data.get('accomplish_time'),
            'feedback_information': feedback_information,
            'create_time': self.fake.date_time_between(start_date='-2y', end_date='now'),
            'update_time': self.fake.date_time_between(start_date='-1y', end_date='now')
        }
        
        return record