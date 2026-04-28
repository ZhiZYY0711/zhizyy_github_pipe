#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
演习和检修员连接表数据生成器
"""

from typing import Dict, Any, List, Set, Tuple
import random
from .base_generator import BaseGenerator
from loguru import logger


class ConnManoeuvreRepairmanGenerator(BaseGenerator):
    """演习和检修员连接表数据生成器"""
    
    def __init__(self, locale: str = 'zh_CN'):
        super().__init__(locale)
        # 用于避免重复的组合
        self.generated_combinations: Set[Tuple[int, int]] = set()
    
    def get_table_name(self) -> str:
        """获取表名"""
        return 'conn_manoeuvre_repairman'
    
    def generate_record(self) -> Dict[str, Any]:
        """生成单条连接记录"""
        # 使用缓存获取有效的外键ID
        manoeuvre_ids = self.get_cached_foreign_keys('manoeuvre')
        repairman_ids = self.get_cached_foreign_keys('repairman')
        
        if not manoeuvre_ids:
            logger.warning("没有找到演习记录，使用默认ID 1")
            manoeuvre_ids = [1]
        
        if not repairman_ids:
            logger.warning("没有找到检修员记录，使用默认ID 1")
            repairman_ids = [1]
        
        # 生成唯一的演习-检修员组合
        max_attempts = 100  # 最大尝试次数，避免无限循环
        attempts = 0
        
        while attempts < max_attempts:
            manoeuvre_id = random.choice(manoeuvre_ids)
            repairman_id = random.choice(repairman_ids)
            combination = (manoeuvre_id, repairman_id)
            
            if combination not in self.generated_combinations:
                self.generated_combinations.add(combination)
                return {
                    'manoeuvre_id': manoeuvre_id,
                    'repairman_id': repairman_id
                }
            
            attempts += 1
        
        # 如果尝试次数过多，清空已生成组合并重新开始
        logger.warning(f"生成唯一组合尝试次数过多，清空已生成组合记录")
        self.generated_combinations.clear()
        
        manoeuvre_id = random.choice(manoeuvre_ids)
        repairman_id = random.choice(repairman_ids)
        combination = (manoeuvre_id, repairman_id)
        self.generated_combinations.add(combination)
        
        return {
            'manoeuvre_id': manoeuvre_id,
            'repairman_id': repairman_id
        }
    
    def generate_batch(self, count: int) -> List[Dict[str, Any]]:
        """
        生成批量记录，重写以处理唯一性约束
        
        Args:
            count: 生成记录数量
            
        Returns:
            记录列表
        """
        # 获取可用的外键数量
        manoeuvre_ids = self.get_cached_foreign_keys('manoeuvre')
        repairman_ids = self.get_cached_foreign_keys('repairman')
        
        if not manoeuvre_ids or not repairman_ids:
            logger.warning("缺少必要的外键数据，无法生成连接记录")
            return []
        
        # 计算理论上的最大唯一组合数
        max_combinations = len(manoeuvre_ids) * len(repairman_ids)
        
        if count > max_combinations:
            logger.warning(f"请求生成数量 {count} 超过最大可能的唯一组合数 {max_combinations}，调整为 {max_combinations}")
            count = max_combinations
        
        # 清空之前的组合记录
        self.generated_combinations.clear()
        
        records = []
        for i in range(count):
            try:
                record = self.generate_record()
                if record:
                    records.append(record)
                else:
                    logger.warning(f"第 {i+1} 条记录生成失败")
                    break
            except Exception as e:
                logger.error(f"生成第 {i+1} 条记录时出错: {e}")
                break
        
        logger.info(f"成功生成 {len(records)} 条演习-检修员连接记录")
        return records
    
    def generate_realistic_connections(self, count: int) -> List[Dict[str, Any]]:
        """
        生成更真实的连接关系
        每个演习分配1-5个检修员，优先分配同区域的检修员
        
        Args:
            count: 期望生成的连接数量
            
        Returns:
            记录列表
        """
        records = []
        
        # 获取演习和检修员数据
        manoeuvre_ids = self.get_cached_foreign_keys('manoeuvre')
        repairman_ids = self.get_cached_foreign_keys('repairman')
        
        if not manoeuvre_ids or not repairman_ids:
            logger.warning("缺少必要的外键数据")
            return []
        
        # 清空之前的组合记录
        self.generated_combinations.clear()
        
        # 为每个演习分配检修员
        for manoeuvre_id in manoeuvre_ids:
            # 每个演习分配1-5个检修员
            num_repairmen = random.randint(1, min(5, len(repairman_ids)))
            
            # 随机选择检修员
            selected_repairmen = random.sample(repairman_ids, num_repairmen)
            
            for repairman_id in selected_repairmen:
                combination = (manoeuvre_id, repairman_id)
                if combination not in self.generated_combinations:
                    self.generated_combinations.add(combination)
                    records.append({
                        'manoeuvre_id': manoeuvre_id,
                        'repairman_id': repairman_id
                    })
                    
                    # 如果达到期望数量就停止
                    if len(records) >= count:
                        break
            
            if len(records) >= count:
                break
        
        logger.info(f"生成了 {len(records)} 条真实的演习-检修员连接记录")
        return records
    
    def generate_and_save_realistic(self, count: int = None, if_exists: str = 'append') -> int:
        """
        生成并保存真实的连接数据
        
        Args:
            count: 生成记录数量
            if_exists: 如果表存在的处理方式
            
        Returns:
            实际保存的记录数量
        """
        if count is None:
            count = 100
        
        logger.info(f"开始生成 {count} 条真实的演习-检修员连接记录")
        
        try:
            records = self.generate_realistic_connections(count)
            if not records:
                logger.warning("没有生成任何记录")
                return 0
            
            saved_count = self.save_to_database(records, if_exists)
            logger.success(f"成功保存 {saved_count} 条演习-检修员连接记录到数据库")
            return saved_count
            
        except Exception as e:
            logger.error(f"生成和保存演习-检修员连接数据时出错: {e}")
            return 0