# -*- coding: utf-8 -*-
"""
日志表数据生成器
"""

from typing import Dict, Any, Optional, List, Tuple
from datetime import datetime, timedelta
import random
import ipaddress
import logging
from .base_generator import BaseGenerator
from loguru import logger


class LogGenerator(BaseGenerator):
    """日志表数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN', custom_config: Optional[Dict[str, Any]] = None):
        """
        初始化日志表生成器
        
        Args:
            locale: 本地化设置，默认中文
            custom_config: 自定义配置，用于覆盖默认随机数据生成规则
        """
        super().__init__(locale)
        self.custom_config = custom_config or {}
        self._setup_config()
        
    def _setup_config(self):
        """设置配置参数"""
        # 用户配置
        self.user_config = self.custom_config.get('user', {})
        self.user_id_range = self.user_config.get('id_range', [1, 1000])
        
        # 管理员和检修员ID范围配置
        self.admin_id_range = self.user_config.get('admin_id_range', [1, 10])
        self.repairman_id_range = self.user_config.get('repairman_id_range', [1, 500])
        
        # 操作类型配置
        self.operation_config = self.custom_config.get('operation', {})
        self.operations = self.operation_config.get('operations', [
            '用户登录', '用户登出', '数据查询', '数据修改', '数据删除', '数据导出',
            '系统配置', '权限管理', '报告生成', '数据备份', '系统监控', '告警处理',
            '任务分配', '任务完成', '设备检修', '传感器校准', '管道巡检', '故障排除'
        ])
        
        # 状态配置
        self.status_config = self.custom_config.get('status', {})
        self.status_weights = self.status_config.get('weights', {
            0: 0.8,  # 成功 - 80%
            1: 0.15, # 失败 - 15%
            2: 0.05  # 未知 - 5%
        })
        
        # IP地址配置
        self.ip_config = self.custom_config.get('ip', {})
        self.ip_ranges = self.ip_config.get('ranges', [
            '192.168.1.0/24',    # 内网段1
            '192.168.10.0/24',   # 内网段2
            '10.0.0.0/24',       # 内网段3
            '172.16.0.0/24'      # 内网段4
        ])
        
        # 时间配置
        self.time_config = self.custom_config.get('time', {})
        self.operation_time_range_days = self.time_config.get('operation_time_range_days', 30)
        self.period_range_minutes = self.time_config.get('period_range_minutes', [1, 120])
        
        # 详情配置
        self.details_config = self.custom_config.get('details', {})
        self.details_templates = self.details_config.get('templates', [
            '操作成功完成',
            '用户从IP {ip} 登录系统',
            '查询了 {count} 条记录',
            '修改了设备 {device_id} 的配置',
            '导出了 {date} 的报告数据',
            '执行了系统备份操作',
            '处理了传感器 {sensor_id} 的告警',
            '完成了管道 {pipeline_id} 的巡检',
            '分配任务给检修员 {repairman_id}',
            '系统配置更新：{config_item}',
            '权限变更：用户 {user_id} 权限调整',
            '数据同步操作完成',
            '设备校准：传感器 {sensor_id}',
            '故障排除：管道段 {section_id}',
            '监控数据异常处理'
        ])
        
    def get_table_name(self) -> str:
        """获取表名"""
        return 'log'
    
    def _generate_type_and_user_id(self) -> Tuple[int, int]:
        """
        生成操作员类型和对应的用户ID
        
        Returns:
            tuple: (type, user_id)
                - type 0: 管理员，user_id范围可配置（默认1-10）
                - type 1: 检修员，user_id范围可配置（默认1-500）
        """
        user_type = random.choice([0, 1])
        
        if user_type == 0:  # 管理员
            user_id = random.randint(self.admin_id_range[0], self.admin_id_range[1])
        elif user_type == 1:  # 检修员
            user_id = random.randint(self.repairman_id_range[0], self.repairman_id_range[1])
        else:
            # 如果有其他类型，使用原始配置范围
            user_id = random.randint(self.user_id_range[0], self.user_id_range[1])
        
        return user_type, user_id
    
    def _generate_operate(self) -> str:
        """生成操作类型"""
        return random.choice(self.operations)
    
    def _generate_status(self) -> int:
        """
        生成操作状态
        0: 成功
        1: 失败
        2: 未知
        """
        statuses = list(self.status_weights.keys())
        weights = list(self.status_weights.values())
        return random.choices(statuses, weights=weights)[0]
    
    def _generate_ip_address(self) -> str:
        """生成IP地址"""
        # 随机选择一个IP段
        ip_range = random.choice(self.ip_ranges)
        network = ipaddress.IPv4Network(ip_range)
        
        # 在该网段内生成随机IP
        # 排除网络地址和广播地址
        hosts = list(network.hosts())
        if hosts:
            return str(random.choice(hosts))
        else:
            # 如果是单个IP，直接返回
            return str(network.network_address)
    
    def _generate_details(self, operate: str, status: int, ip: str) -> Optional[str]:
        """
        生成操作详情
        
        Args:
            operate: 操作类型
            status: 操作状态
            ip: IP地址
        """
        if random.random() < 0.3:  # 30%的概率不生成详情
            return None
            
        # 根据操作状态选择模板
        if status == 1:  # 失败状态
            error_templates = [
                f'操作失败：{operate}',
                f'权限不足，无法执行{operate}',
                f'系统错误，{operate}操作中断',
                f'网络异常，{operate}操作超时',
                f'数据验证失败，{operate}操作被拒绝'
            ]
            return random.choice(error_templates)
        elif status == 2:  # 未知状态
            unknown_templates = [
                f'{operate}操作状态未知',
                f'系统响应异常，{operate}结果待确认',
                f'网络中断，{operate}操作结果不明'
            ]
            return random.choice(unknown_templates)
        else:  # 成功状态
            template = random.choice(self.details_templates)
            
            # 替换模板中的占位符
            replacements = {
                'ip': ip,
                'count': random.randint(1, 1000),
                'device_id': random.randint(1, 100),
                'sensor_id': random.randint(1, 500),
                'pipeline_id': random.randint(1, 50),
                'repairman_id': random.randint(1, 100),
                'user_id': random.randint(1, 200),
                'section_id': f'P{random.randint(1, 20):03d}',
                'config_item': random.choice(['系统参数', '告警阈值', '备份策略', '监控频率']),
                'date': self.fake.date_between(start_date='-30d', end_date='today').strftime('%Y-%m-%d')
            }
            
            for key, value in replacements.items():
                template = template.replace(f'{{{key}}}', str(value))
                
            return template
    
    def _generate_period(self) -> datetime:
        """生成持续时间（这里用datetime表示持续的分钟数）"""
        # 生成1-120分钟的随机持续时间
        minutes = random.randint(self.period_range_minutes[0], self.period_range_minutes[1])
        # 使用一个基准时间加上分钟数来表示持续时间
        base_time = datetime(1970, 1, 1)
        return base_time + timedelta(minutes=minutes)
    
    def _generate_operation_time(self) -> datetime:
        """生成操作时间"""
        # 在指定天数范围内生成随机时间
        end_date = datetime.now()
        start_date = end_date - timedelta(days=self.operation_time_range_days)
        
        # 生成随机时间戳
        time_between = end_date - start_date
        random_seconds = random.randint(0, int(time_between.total_seconds()))
        
        return start_date + timedelta(seconds=random_seconds)
    
    def _generate_create_time(self) -> datetime:
        """生成创建时间"""
        # 创建时间通常在操作时间之后的几分钟内
        operation_time = self._generate_operation_time()
        delay_minutes = random.randint(0, 5)
        return operation_time + timedelta(minutes=delay_minutes)
    
    def _generate_update_time(self, create_time: datetime) -> datetime:
        """生成更新时间"""
        # 更新时间通常等于创建时间或稍后
        if random.random() < 0.8:  # 80%的概率更新时间等于创建时间
            return create_time
        else:
            # 20%的概率更新时间稍后于创建时间
            delay_minutes = random.randint(1, 30)
            return create_time + timedelta(minutes=delay_minutes)
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条日志记录"""
        try:
            # 生成基础字段
            user_type, user_id = self._generate_type_and_user_id()
            operate = self._generate_operate()
            status = self._generate_status()
            ip_address = self._generate_ip_address()
            operation_time = self._generate_operation_time()
            create_time = self._generate_create_time()
            
            # 生成依赖字段
            details = self._generate_details(operate, status, ip_address)
            period = self._generate_period()
            update_time = self._generate_update_time(create_time)
            
            record = {
                'user_id': user_id,
                'type': user_type,
                'operate': operate,
                'status': status,
                'ip_address': ip_address,
                'details': details,
                'period': period,
                'operation_time': operation_time,
                'create_time': create_time,
                'update_time': update_time
            }
            
            logger.debug(f"生成日志记录: user_id={user_id}, operate={operate}, status={status}")
            return record
            
        except Exception as e:
            logger.error(f"生成日志记录时出错: {e}")
            raise
    
    def get_field_info(self) -> Dict[str, str]:
        """获取字段信息"""
        return {
            'user_id': '管理员或检修员的ID',
            'type': '操作员类型 (0:管理员, 1:检修员)',
            'operate': '进行的操作',
            'status': '操作状态 (0:成功, 1:失败, 2:未知)',
            'ip_address': '操作者的IP地址',
            'details': '操作详情',
            'period': '持续时间',
            'operation_time': '操作时间',
            'create_time': '创建时间',
            'update_time': '更新时间'
        }
    
    def get_sample_config(self) -> Dict[str, Any]:
        """获取配置示例"""
        return {
            'user': {
                'id_range': [1, 1000],
                'admin_id_range': [1, 10],
                'repairman_id_range': [1, 500]
            },
            'operation': {
                'operations': [
                    '用户登录', '用户登出', '数据查询', '数据修改', 
                    '设备检修', '传感器校准', '管道巡检'
                ]
            },
            'status': {
                'weights': {
                    0: 0.8,  # 成功
                    1: 0.15, # 失败
                    2: 0.05  # 未知
                }
            },
            'ip': {
                'ranges': [
                    '192.168.1.0/24',
                    '10.0.0.0/24'
                ]
            },
            'time': {
                'operation_time_range_days': 30,
                'period_range_minutes': [1, 120]
            }
        }
    
    def validate_record(self, record: Dict[str, Any]) -> bool:
        """
        验证记录的有效性
        
        Args:
            record: 要验证的记录
            
        Returns:
            是否有效
        """
        try:
            # 检查必需字段
            required_fields = ['user_id', 'type', 'operate', 'status', 'ip_address', 
                             'period', 'operation_time', 'create_time', 'update_time']
            
            for field in required_fields:
                if field not in record:
                    logger.warning(f"记录缺少必需字段: {field}")
                    return False
            
            # 验证字段值范围
            if record['type'] not in [0, 1]:
                logger.warning(f"无效的操作员类型: {record['type']}")
                return False
                
            if record['status'] not in [0, 1, 2]:
                logger.warning(f"无效的操作状态: {record['status']}")
                return False
            
            # 验证IP地址格式
            try:
                ipaddress.IPv4Address(record['ip_address'])
            except ipaddress.AddressValueError:
                logger.warning(f"无效的IP地址: {record['ip_address']}")
                return False
            
            # 验证时间字段
            time_fields = ['period', 'operation_time', 'create_time', 'update_time']
            for field in time_fields:
                if not isinstance(record[field], datetime):
                    logger.warning(f"无效的时间字段 {field}: {record[field]}")
                    return False
            
            return True
            
        except Exception as e:
            logger.error(f"验证记录时出错: {e}")
            return False