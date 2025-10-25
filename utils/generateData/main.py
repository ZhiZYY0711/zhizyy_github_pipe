#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
管道监测系统数据生成器 - 统一主程序
支持交互式选择生成不同表的数据
"""

import sys
import os
from pathlib import Path
import argparse
from typing import Dict, Any, Optional

# 添加项目根目录到Python路径
current_dir = Path(__file__).parent
src_dir = current_dir / "src"
sys.path.insert(0, str(src_dir))

from generators import (
    PipelineGenerator,
    InspectionGenerator, 
    RepairmanRegistrationGenerator,
    SensorGenerator
)
from logger import setup_logger
from loguru import logger
from config import config


class DataGeneratorManager:
    """数据生成器管理器"""
    
    def __init__(self):
        """初始化生成器管理器"""
        self.generators = {
            'pipeline': {
                'name': '管道数据',
                'class': PipelineGenerator,
                'description': '生成管道基础信息数据'
            },
            'sensor': {
                'name': '传感器数据', 
                'class': SensorGenerator,
                'description': '生成传感器设备数据'
            },
            'inspection': {
                'name': '巡检数据',
                'class': InspectionGenerator,
                'description': '生成巡检监测数据'
            },
            'repairman_registration': {
                'name': '检修员登录数据',
                'class': RepairmanRegistrationGenerator,
                'description': '生成检修员登录记录数据'
            }
        }
    
    def show_menu(self):
        """显示主菜单"""
        print("\n" + "="*60)
        print("           管道监测系统数据生成器")
        print("="*60)
        print("请选择要生成的数据类型:")
        print()
        
        for key, info in self.generators.items():
            print(f"  {key:25} - {info['name']}")
            print(f"  {' '*25}   {info['description']}")
            print()
        
        print("  all                     - 生成所有类型数据")
        print("  exit                    - 退出程序")
        print("="*60)
    
    def get_generation_options(self) -> Dict[str, Any]:
        """获取生成选项"""
        options = {}
        
        # 获取生成数量
        while True:
            try:
                count_input = input("请输入生成数量 (默认: 100): ").strip()
                if not count_input:
                    options['count'] = 100
                    break
                count = int(count_input)
                if count <= 0:
                    print("数量必须大于0，请重新输入")
                    continue
                options['count'] = count
                break
            except ValueError:
                print("请输入有效的数字")
        
        # 获取处理方式
        print("\n数据处理方式:")
        print("  append  - 追加到现有数据 (默认)")
        print("  replace - 替换现有数据")
        print("  fail    - 如果表存在则失败")
        
        while True:
            if_exists = input("请选择处理方式 (append/replace/fail): ").strip().lower()
            if not if_exists:
                options['if_exists'] = 'append'
                break
            if if_exists in ['append', 'replace', 'fail']:
                options['if_exists'] = if_exists
                break
            print("无效选择，请输入 append、replace 或 fail")
        
        return options
    
    def generate_data(self, generator_key: str, options: Dict[str, Any]) -> bool:
        """
        生成指定类型的数据
        
        Args:
            generator_key: 生成器键名
            options: 生成选项
            
        Returns:
            是否成功
        """
        try:
            if generator_key not in self.generators:
                logger.error(f"未知的生成器类型: {generator_key}")
                return False
            
            generator_info = self.generators[generator_key]
            generator_class = generator_info['class']
            
            logger.info(f"开始生成 {generator_info['name']}")
            
            # 创建生成器实例
            if generator_key == 'repairman_registration':
                # 检修员登录数据使用特殊的生成方法
                generator = generator_class()
                saved_count = generator.generate_and_save_unique(
                    count=options['count'],
                    if_exists=options['if_exists']
                )
            else:
                generator = generator_class()
                saved_count = generator.generate_and_save(
                    count=options['count'],
                    if_exists=options['if_exists']
                )
            
            logger.success(f"{generator_info['name']} 生成完成，共保存 {saved_count} 条记录")
            return True
            
        except Exception as e:
            logger.error(f"生成 {generator_key} 数据时出错: {e}")
            return False
    
    def generate_all_data(self, options: Dict[str, Any]) -> bool:
        """
        生成所有类型的数据
        
        Args:
            options: 生成选项
            
        Returns:
            是否全部成功
        """
        logger.info("开始生成所有类型数据")
        
        # 按依赖关系排序：管道 -> 传感器 -> 巡检数据，检修员登录数据独立
        generation_order = ['pipeline', 'sensor', 'inspection', 'repairman_registration']
        
        success_count = 0
        total_count = len(generation_order)
        
        for generator_key in generation_order:
            print(f"\n{'='*50}")
            print(f"正在生成: {self.generators[generator_key]['name']}")
            print('='*50)
            
            if self.generate_data(generator_key, options):
                success_count += 1
            else:
                logger.warning(f"{self.generators[generator_key]['name']} 生成失败")
        
        logger.info(f"批量生成完成: {success_count}/{total_count} 个类型成功")
        return success_count == total_count
    
    def show_table_status(self):
        """显示各表的当前记录数"""
        print("\n" + "="*50)
        print("           数据表状态")
        print("="*50)
        
        for key, info in self.generators.items():
            try:
                generator = info['class']()
                count = generator.get_table_count()
                print(f"  {info['name']:15} : {count:>8} 条记录")
            except Exception as e:
                print(f"  {info['name']:15} : 获取失败 ({e})")
        
        print("="*50)
    
    def run_interactive(self):
        """运行交互式模式"""
        logger.info("启动交互式数据生成器")
        
        while True:
            try:
                self.show_menu()
                self.show_table_status()
                
                choice = input("\n请输入选择: ").strip().lower()
                
                if choice == 'exit':
                    print("感谢使用，再见！")
                    break
                
                if choice == 'all':
                    print("\n生成所有类型数据")
                    options = self.get_generation_options()
                    self.generate_all_data(options)
                    
                elif choice in self.generators:
                    generator_info = self.generators[choice]
                    print(f"\n生成 {generator_info['name']}")
                    options = self.get_generation_options()
                    self.generate_data(choice, options)
                    
                else:
                    print("无效选择，请重新输入")
                    continue
                
                input("\n按回车键继续...")
                
            except KeyboardInterrupt:
                print("\n\n程序被用户中断")
                break
            except Exception as e:
                logger.error(f"程序运行出错: {e}")
                input("按回车键继续...")


def load_custom_config(config_path: Optional[str] = None) -> Optional[Dict[str, Any]]:
    """
    加载自定义配置文件
    
    Args:
        config_path: 配置文件路径
        
    Returns:
        配置字典或None
    """
    if not config_path:
        return None
        
    try:
        import yaml
        with open(config_path, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)
        logger.info(f"成功加载自定义配置文件: {config_path}")
        return config
    except Exception as e:
        logger.error(f"加载配置文件失败: {e}")
        return None


def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description='管道监测系统数据生成器',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  python main.py                          # 交互式模式
  python main.py --type pipeline --count 100    # 生成100条管道数据
  python main.py --type all --count 50          # 生成所有类型数据，每种50条
  python main.py --template                     # 显示配置模板
        """
    )
    
    parser.add_argument(
        '--type', '-t',
        choices=['pipeline', 'sensor', 'inspection', 'repairman_registration', 'all'],
        help='指定生成数据类型'
    )
    
    parser.add_argument(
        '--count', '-c',
        type=int,
        default=100,
        help='生成记录数量 (默认: 100)'
    )
    
    parser.add_argument(
        '--if-exists',
        choices=['append', 'replace', 'fail'],
        default='append',
        help='如果表存在的处理方式 (默认: append)'
    )
    
    parser.add_argument(
        '--config',
        help='自定义配置文件路径'
    )
    
    parser.add_argument(
        '--template',
        action='store_true',
        help='显示配置文件模板'
    )
    
    parser.add_argument(
        '--log-level',
        choices=['DEBUG', 'INFO', 'WARNING', 'ERROR'],
        default='INFO',
        help='日志级别 (默认: INFO)'
    )
    
    args = parser.parse_args()
    
    # 设置日志
    setup_logger()
    
    # 显示配置模板
    if args.template:
        show_config_template()
        return
    
    try:
        # 加载自定义配置
        custom_config = load_custom_config(args.config)
        
        # 创建管理器
        manager = DataGeneratorManager()
        
        # 命令行模式
        if args.type:
            options = {
                'count': args.count,
                'if_exists': args.if_exists
            }
            
            if args.type == 'all':
                success = manager.generate_all_data(options)
            else:
                success = manager.generate_data(args.type, options)
            
            if success:
                logger.success("数据生成完成")
            else:
                logger.error("数据生成失败")
                sys.exit(1)
        else:
            # 交互式模式
            manager.run_interactive()
            
    except Exception as e:
        logger.error(f"程序执行失败: {e}")
        sys.exit(1)


def show_config_template():
    """显示配置文件模板"""
    template = """
# 管道监测系统数据生成器配置模板

# 检修员登录数据配置示例
repairman_registration:
  username:
    prefix: "repair"           # 用户名前缀
    suffix_length: 3           # 后缀长度
    patterns:                  # 预定义用户名模式
      - "admin001"
      - "tech002"
      - "manager003"
  
  password:
    default: "123456"          # 默认密码
    patterns:                  # 预定义密码模式
      - "123456"
      - "admin123"
      - "tech456"
  
  time:
    login_time_range_days: 30      # 登录时间范围（天）
    password_update_range_days: 90  # 密码更新时间范围（天）
  
  data:
    default_count: 100         # 默认生成数量

# 其他生成器配置可以在这里添加...
"""
    print(template)


if __name__ == "__main__":
    main()