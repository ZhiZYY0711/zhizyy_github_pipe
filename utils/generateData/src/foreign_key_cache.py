# -*- coding: utf-8 -*-
"""
外键ID缓存管理器
用于缓存数据库中的外键ID，避免重复查询，提升数据生成性能
"""

from typing import Dict, List, Optional
from loguru import logger
import threading
import time


class ForeignKeyCache:
    """外键ID缓存管理器"""
    
    def __init__(self, cache_ttl: int = 300):
        """
        初始化缓存管理器
        
        Args:
            cache_ttl: 缓存过期时间（秒），默认5分钟
        """
        self._cache: Dict[str, Dict] = {}
        self._cache_ttl = cache_ttl
        self._lock = threading.Lock()
        
    def get_foreign_keys(self, table_name: str, limit: int = 1000) -> List[int]:
        """
        获取指定表的外键ID列表
        
        Args:
            table_name: 表名
            limit: 查询限制数量
            
        Returns:
            外键ID列表
        """
        cache_key = f"{table_name}_{limit}"
        
        with self._lock:
            # 检查缓存是否存在且未过期
            if cache_key in self._cache:
                cache_data = self._cache[cache_key]
                if time.time() - cache_data['timestamp'] < self._cache_ttl:
                    logger.debug(f"从缓存获取 {table_name} 外键ID，共 {len(cache_data['ids'])} 个")
                    return cache_data['ids']
                else:
                    # 缓存过期，删除
                    del self._cache[cache_key]
                    logger.debug(f"{table_name} 缓存已过期，重新查询")
            
            # 缓存不存在或已过期，从数据库查询
            ids = self._fetch_from_database(table_name, limit)
            
            # 更新缓存
            self._cache[cache_key] = {
                'ids': ids,
                'timestamp': time.time()
            }
            
            logger.info(f"从数据库查询并缓存 {table_name} 外键ID，共 {len(ids)} 个")
            return ids
    
    def _fetch_from_database(self, table_name: str, limit: int) -> List[int]:
        """
        从数据库查询外键ID
        
        Args:
            table_name: 表名
            limit: 查询限制数量
            
        Returns:
            外键ID列表
        """
        try:
            from database import DatabaseManager
            db_manager = DatabaseManager()
            
            query = f"SELECT id FROM {table_name} LIMIT {limit}"
            result = db_manager.execute_query(query)
            
            if result:
                return [row['id'] for row in result]
            else:
                # 返回默认值
                return self._get_default_ids(table_name)
                
        except Exception as e:
            logger.error(f"查询 {table_name} 外键ID时出错: {e}")
            return self._get_default_ids(table_name)
    
    def _get_default_ids(self, table_name: str) -> List[int]:
        """
        获取默认的外键ID列表
        
        Args:
            table_name: 表名
            
        Returns:
            默认ID列表
        """
        default_mapping = {
            'area': [1, 2, 3, 4, 5],
            'pipe': [1, 2, 3],
            'repairman': [1, 2, 3, 4, 5],
            'sensor': [1, 2, 3],
            'pipeline': [1, 2, 3]
        }
        
        return default_mapping.get(table_name, [1, 2, 3])
    
    def clear_cache(self, table_name: Optional[str] = None):
        """
        清空缓存
        
        Args:
            table_name: 指定表名清空，None则清空所有缓存
        """
        with self._lock:
            if table_name:
                # 清空指定表的缓存
                keys_to_remove = [key for key in self._cache.keys() if key.startswith(table_name)]
                for key in keys_to_remove:
                    del self._cache[key]
                logger.info(f"已清空 {table_name} 相关缓存")
            else:
                # 清空所有缓存
                self._cache.clear()
                logger.info("已清空所有外键缓存")
    
    def get_cache_info(self) -> Dict[str, Dict]:
        """
        获取缓存信息
        
        Returns:
            缓存信息字典
        """
        with self._lock:
            cache_info = {}
            current_time = time.time()
            
            for key, data in self._cache.items():
                remaining_ttl = self._cache_ttl - (current_time - data['timestamp'])
                cache_info[key] = {
                    'count': len(data['ids']),
                    'remaining_ttl': max(0, remaining_ttl),
                    'expired': remaining_ttl <= 0
                }
            
            return cache_info


# 全局缓存实例
foreign_key_cache = ForeignKeyCache()