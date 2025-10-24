#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库连接测试脚本
"""

import sys
import os

# 添加src目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from loguru import logger
from logger import setup_logger
from database import db_manager
from config import config


def test_config():
    """测试配置加载"""
    logger.info("测试配置加载...")
    
    try:
        db_config = config.database
        logger.info(f"数据库主机: {db_config.host}")
        logger.info(f"数据库端口: {db_config.port}")
        logger.info(f"数据库名称: {db_config.database}")
        logger.info(f"数据库用户: {db_config.user}")
        logger.info("配置加载成功！")
        return True
    except Exception as e:
        logger.error(f"配置加载失败: {e}")
        return False


def test_database_connection():
    """测试数据库连接"""
    logger.info("测试数据库连接...")
    
    try:
        if db_manager.test_connection():
            logger.info("数据库连接成功！")
            return True
        else:
            logger.error("数据库连接失败！")
            return False
    except Exception as e:
        logger.error(f"数据库连接测试出错: {e}")
        return False


def test_database_operations():
    """测试数据库基本操作"""
    logger.info("测试数据库基本操作...")
    
    try:
        # 获取所有表
        tables = db_manager.get_tables()
        logger.info(f"数据库中的表: {tables}")
        
        # 测试查询操作
        result = db_manager.execute_query("SELECT 1 as test")
        logger.info(f"测试查询结果: {result}")
        
        # 测试表存在检查
        for table in ['area', 'pipeline', 'inspection']:
            exists = db_manager.table_exists(table)
            logger.info(f"表 {table} 存在: {exists}")
            
            if exists:
                count = db_manager.get_table_count(table)
                logger.info(f"表 {table} 记录数: {count}")
                
                # 获取表结构
                table_info = db_manager.get_table_info(table)
                logger.info(f"表 {table} 结构:")
                for column in table_info:
                    field_name = column.get('Field', column.get('field', 'Unknown'))
                    field_type = column.get('Type', column.get('type', 'Unknown'))
                    logger.info(f"  {field_name}: {field_type}")
        
        logger.info("数据库操作测试成功！")
        return True
        
    except Exception as e:
        logger.error(f"数据库操作测试失败: {e}")
        return False


def main():
    """主函数"""
    logger.info("=" * 50)
    logger.info("数据库连接测试")
    logger.info("=" * 50)
    
    # 测试配置
    if not test_config():
        logger.error("配置测试失败，请检查配置文件")
        return
    
    # 测试数据库连接
    if not test_database_connection():
        logger.error("数据库连接测试失败，请检查数据库配置和连接")
        return
    
    # 测试数据库操作
    if not test_database_operations():
        logger.error("数据库操作测试失败")
        return
    
    logger.info("所有测试通过！")


if __name__ == "__main__":
    main()