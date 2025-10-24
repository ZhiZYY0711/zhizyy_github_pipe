#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
示例数据生成脚本
"""

import sys
import os

# 添加src目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))
# 添加generators目录到Python路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src', 'generators'))

from loguru import logger
from logger import setup_logger
from database import db_manager
from generators import AreaGenerator, PipelineGenerator, InspectionGenerator
from sensor_generator import SensorGenerator


def test_database_connection():
    """测试数据库连接"""
    logger.info("测试数据库连接...")
    
    if db_manager.test_connection():
        logger.info("数据库连接成功！")
        
        # 显示数据库中的表
        tables = db_manager.get_tables()
        logger.info(f"数据库中的表: {tables}")
        
        return True
    else:
        logger.error("数据库连接失败！")
        return False


def generate_area_data(count: int = 100):
    """生成区域数据"""
    logger.info(f"开始生成 {count} 条区域数据...")
    
    generator = AreaGenerator()
    
    # 检查表是否存在
    if not db_manager.table_exists(generator.get_table_name()):
        logger.error(f"表 {generator.get_table_name()} 不存在，请先创建表结构")
        return
    
    # 生成并保存数据
    saved_count = generator.generate_and_save(count)
    logger.info(f"区域数据生成完成，保存了 {saved_count} 条记录")


def generate_pipeline_data(count: int = 50):
    """生成管道数据"""
    logger.info(f"开始生成 {count} 条管道数据...")
    
    generator = PipelineGenerator()
    
    # 检查表是否存在
    if not db_manager.table_exists(generator.get_table_name()):
        logger.error(f"表 {generator.get_table_name()} 不存在，请先创建表结构")
        return
    
    # 生成并保存数据
    saved_count = generator.generate_and_save(count)
    logger.info(f"管道数据生成完成，保存了 {saved_count} 条记录")


def generate_sensor_data(count: int = 30):
    """生成传感器数据"""
    logger.info(f"开始生成 {count} 条传感器数据...")
    
    generator = SensorGenerator()
    
    # 检查表是否存在
    if not db_manager.table_exists(generator.get_table_name()):
        logger.error(f"表 {generator.get_table_name()} 不存在，请先创建表结构")
        return
    
    # 生成并保存数据
    saved_count = generator.generate_and_save(count)
    logger.info(f"传感器数据生成完成，保存了 {saved_count} 条记录")


def generate_inspection_data(count: int = 200):
    """生成巡检数据"""
    logger.info(f"开始生成 {count} 条巡检数据...")
    
    generator = InspectionGenerator()
    
    # 检查表是否存在
    if not db_manager.table_exists(generator.get_table_name()):
        logger.error(f"表 {generator.get_table_name()} 不存在，请先创建表结构")
        return
    
    # 生成并保存数据
    saved_count = generator.generate_and_save(count)
    logger.info(f"巡检数据生成完成，保存了 {saved_count} 条记录")


def show_table_stats():
    """显示表统计信息"""
    logger.info("数据库表统计信息:")
    
    generators = [
        AreaGenerator(),
        PipelineGenerator(),
        InspectionGenerator()
    ]
    
    for generator in generators:
        table_name = generator.get_table_name()
        if db_manager.table_exists(table_name):
            count = generator.get_table_count()
            logger.info(f"  {table_name}: {count} 条记录")
        else:
            logger.warning(f"  {table_name}: 表不存在")


def main():
    """主函数"""
    logger.info("=" * 50)
    logger.info("管道监测管理系统 - 示例数据生成")
    logger.info("=" * 50)
    
    # 测试数据库连接
    if not test_database_connection():
        return
    
    # 显示当前表状态
    show_table_stats()
    
    # 生成示例数据
    try:
        # 生成区域数据
        generate_area_data(100)
        
        # 生成管道数据
        generate_pipeline_data(50)
        
        # 生成传感器数据
        generate_sensor_data(30)
        
        # 生成巡检数据
        generate_inspection_data(200)
        
        # 显示最终统计
        logger.info("\n" + "=" * 30)
        logger.info("数据生成完成！最终统计:")
        show_table_stats()
        
    except Exception as e:
        logger.error(f"数据生成过程中出现错误: {e}")
        raise


if __name__ == "__main__":
    main()