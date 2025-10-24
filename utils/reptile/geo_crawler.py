#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
地理边界数据爬虫
从阿里云DataV获取中国行政区划边界数据
"""

import requests
import json
import os
import time
from pathlib import Path
from typing import Dict, List, Tuple
import logging

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('crawler.log', encoding='utf-8'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)


class GeoBoundaryCrawler:
    """地理边界数据爬虫类"""
    
    def __init__(self, code_file_path: str, output_dir: str = "geo_data"):
        """
        初始化爬虫
        
        Args:
            code_file_path: 区划代码文件路径
            output_dir: 输出目录
        """
        self.code_file_path = code_file_path
        self.output_dir = Path(output_dir)
        self.base_url = "https://geo.datav.aliyun.com/areas_v3/bound"
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        })
        
        # 创建输出目录
        self.output_dir.mkdir(exist_ok=True)
        
        # 统计信息
        self.stats = {
            'total': 0,
            'success': 0,
            'failed': 0,
            'skipped': 0
        }
    
    def load_area_codes(self) -> List[Tuple[str, str]]:
        """
        加载区划代码
        
        Returns:
            List[Tuple[str, str]]: [(代码, 名称), ...]
        """
        area_codes = []
        try:
            with open(self.code_file_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line:
                        parts = line.split(' ', 1)
                        if len(parts) == 2:
                            code, name = parts
                            area_codes.append((code, name))
            
            logger.info(f"成功加载 {len(area_codes)} 个区划代码")
            return area_codes
            
        except Exception as e:
            logger.error(f"加载区划代码失败: {e}")
            return []
    
    def get_area_level(self, code: str) -> str:
        """
        判断区划级别
        
        Args:
            code: 区划代码
            
        Returns:
            str: 'province', 'city', 'district'
        """
        if code.endswith('0000'):
            return 'province'
        elif code.endswith('00'):
            return 'city'
        else:
            return 'district'
    
    def build_url(self, code: str) -> str:
        """
        构建下载URL
        
        Args:
            code: 区划代码
            
        Returns:
            str: 完整的URL
        """
        level = self.get_area_level(code)
        
        if level == 'district':
            # 区级不使用full，且不要下划线
            return f"{self.base_url}/{code}.json"
        else:
            # 省级和市级使用full
            return f"{self.base_url}/{code}_full.json"
    
    def download_geo_data(self, code: str, name: str) -> bool:
        """
        下载地理数据
        
        Args:
            code: 区划代码
            name: 区划名称
            
        Returns:
            bool: 是否成功
        """
        url = self.build_url(code)
        level = self.get_area_level(code)
        
        # 创建对应级别的目录
        level_dir = self.output_dir / level
        level_dir.mkdir(exist_ok=True)
        
        # 文件路径
        filename = f"{code}_{name}.json"
        # 移除文件名中的非法字符
        filename = "".join(c for c in filename if c.isalnum() or c in (' ', '-', '_', '.')).rstrip()
        file_path = level_dir / filename
        
        # 如果文件已存在，跳过
        if file_path.exists():
            logger.info(f"文件已存在，跳过: {filename}")
            self.stats['skipped'] += 1
            return True
        
        try:
            logger.info(f"正在下载: {name} ({code}) - {url}")
            
            response = self.session.get(url, timeout=30)
            response.raise_for_status()
            
            # 验证是否为有效的JSON
            geo_data = response.json()
            
            # 保存文件
            with open(file_path, 'w', encoding='utf-8') as f:
                json.dump(geo_data, f, ensure_ascii=False, indent=2)
            
            logger.info(f"下载成功: {filename}")
            self.stats['success'] += 1
            return True
            
        except requests.exceptions.RequestException as e:
            logger.error(f"网络请求失败 {name} ({code}): {e}")
            self.stats['failed'] += 1
            return False
            
        except json.JSONDecodeError as e:
            logger.error(f"JSON解析失败 {name} ({code}): {e}")
            self.stats['failed'] += 1
            return False
            
        except Exception as e:
            logger.error(f"下载失败 {name} ({code}): {e}")
            self.stats['failed'] += 1
            return False
    
    def crawl_all(self, delay: float = 1.0, max_retries: int = 3):
        """
        爬取所有地理数据
        
        Args:
            delay: 请求间隔（秒）
            max_retries: 最大重试次数
        """
        area_codes = self.load_area_codes()
        if not area_codes:
            logger.error("没有可用的区划代码")
            return
        
        self.stats['total'] = len(area_codes)
        logger.info(f"开始爬取 {self.stats['total']} 个区划的地理数据")
        
        failed_codes = []
        
        for i, (code, name) in enumerate(area_codes, 1):
            logger.info(f"进度: {i}/{self.stats['total']} - {name}")
            
            # 重试机制
            success = False
            for retry in range(max_retries):
                if self.download_geo_data(code, name):
                    success = True
                    break
                else:
                    if retry < max_retries - 1:
                        logger.warning(f"重试 {retry + 1}/{max_retries}: {name}")
                        time.sleep(delay * 2)  # 重试时延长等待时间
            
            if not success:
                failed_codes.append((code, name))
            
            # 请求间隔
            if i < len(area_codes):
                time.sleep(delay)
        
        # 输出统计信息
        self.print_stats()
        
        # 输出失败的代码
        if failed_codes:
            logger.warning(f"以下 {len(failed_codes)} 个区划下载失败:")
            for code, name in failed_codes:
                logger.warning(f"  {code} - {name}")
    
    def print_stats(self):
        """打印统计信息"""
        logger.info("=" * 50)
        logger.info("爬取统计:")
        logger.info(f"总数: {self.stats['total']}")
        logger.info(f"成功: {self.stats['success']}")
        logger.info(f"失败: {self.stats['failed']}")
        logger.info(f"跳过: {self.stats['skipped']}")
        logger.info(f"成功率: {self.stats['success'] / self.stats['total'] * 100:.1f}%")
        logger.info("=" * 50)


def main():
    """主函数"""
    # 配置参数
    code_file = "code.txt"
    output_dir = "geo_data"
    request_delay = 1.0  # 请求间隔（秒）
    
    # 创建爬虫实例
    crawler = GeoBoundaryCrawler(code_file, output_dir)
    
    # 开始爬取
    try:
        crawler.crawl_all(delay=request_delay)
    except KeyboardInterrupt:
        logger.info("用户中断爬取")
        crawler.print_stats()
    except Exception as e:
        logger.error(f"爬取过程中发生错误: {e}")
        crawler.print_stats()


if __name__ == "__main__":
    main()