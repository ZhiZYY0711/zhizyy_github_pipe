# -*- coding: utf-8 -*-
"""
基础数据生成器
"""

from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
import pandas as pd
from faker import Faker
from loguru import logger
import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(__file__)))
from database import db_manager
from config import config


class BaseGenerator(ABC):
    """基础数据生成器抽象类"""
    
    def __init__(self, locale: str = 'zh_CN'):
        """
        初始化生成器
        
        Args:
            locale: 本地化设置，默认中文
        """
        self.fake = Faker(locale)
        self.batch_size = config.app.batch_size
        self.max_records = config.app.max_records
        
    @abstractmethod
    def generate_record(self) -> Dict[str, Any]:
        """生成单条记录"""
        pass
    
    @abstractmethod
    def get_table_name(self) -> str:
        """获取表名"""
        pass
    
    def generate_batch(self, count: int) -> List[Dict[str, Any]]:
        """
        生成批量记录
        
        Args:
            count: 生成记录数量
            
        Returns:
            记录列表
        """
        records = []
        for i in range(count):
            try:
                record = self.generate_record()
                records.append(record)
                
                if (i + 1) % 100 == 0:
                    logger.debug(f"已生成 {i + 1} 条记录")
                    
            except Exception as e:
                logger.error(f"生成第 {i + 1} 条记录时出错: {e}")
                continue
                
        logger.info(f"批量生成完成，共生成 {len(records)} 条记录")
        return records
    
    def save_to_database(self, records: List[Dict[str, Any]], 
                        if_exists: str = 'append') -> int:
        """
        保存记录到数据库
        
        Args:
            records: 记录列表
            if_exists: 如果表存在的处理方式 ('append', 'replace', 'fail')
            
        Returns:
            插入的记录数
        """
        if not records:
            logger.warning("没有记录需要保存")
            return 0
            
        try:
            df = pd.DataFrame(records)
            table_name = self.get_table_name()
            
            # 检查表是否存在
            if not db_manager.table_exists(table_name):
                logger.warning(f"表 {table_name} 不存在，请先创建表结构")
                return 0
            
            rows_inserted = db_manager.insert_dataframe(
                df=df,
                table_name=table_name,
                if_exists=if_exists,
                chunk_size=self.batch_size
            )
            
            logger.info(f"成功保存 {rows_inserted} 条记录到表 {table_name}")
            return rows_inserted
            
        except Exception as e:
            logger.error(f"保存记录到数据库失败: {e}")
            raise
    
    def generate_and_save(self, count: Optional[int] = None, 
                         if_exists: str = 'append') -> int:
        """
        生成并保存数据
        
        Args:
            count: 生成记录数，默认使用配置中的max_records
            if_exists: 如果表存在的处理方式
            
        Returns:
            保存的记录数
        """
        if count is None:
            count = self.max_records
            
        logger.info(f"开始生成 {count} 条 {self.get_table_name()} 记录")
        
        total_saved = 0
        remaining = count
        
        while remaining > 0:
            batch_size = min(self.batch_size, remaining)
            
            # 生成批量数据
            records = self.generate_batch(batch_size)
            
            if records:
                # 保存到数据库
                saved = self.save_to_database(records, if_exists)
                total_saved += saved
                
                # 第一批之后使用append模式
                if if_exists != 'append':
                    if_exists = 'append'
            
            remaining -= batch_size
            logger.info(f"进度: {count - remaining}/{count}")
        
        logger.info(f"数据生成完成，共保存 {total_saved} 条记录")
        return total_saved
    
    def clear_table(self) -> int:
        """清空表数据"""
        table_name = self.get_table_name()
        
        if not db_manager.table_exists(table_name):
            logger.warning(f"表 {table_name} 不存在")
            return 0
            
        sql = f"DELETE FROM {table_name}"
        affected_rows = db_manager.execute_update(sql)
        logger.info(f"已清空表 {table_name}，删除 {affected_rows} 条记录")
        return affected_rows
    
    def get_table_count(self) -> int:
        """获取表记录数"""
        table_name = self.get_table_name()
        
        if not db_manager.table_exists(table_name):
            return 0
            
        return db_manager.get_table_count(table_name)