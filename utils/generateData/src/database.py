# -*- coding: utf-8 -*-
"""
数据库连接管理模块
"""

import pymysql
import mysql.connector
from mysql.connector import pooling
from typing import Optional, Dict, Any, List, Tuple
from contextlib import contextmanager
from loguru import logger
import pandas as pd
from config import config


class DatabaseManager:
    """数据库管理器"""
    
    def __init__(self):
        self.pool = None
        self._init_connection_pool()
    
    def _init_connection_pool(self):
        """初始化连接池"""
        try:
            db_config = config.database
            pool_config = config.pool
            
            pool_config_dict = {
                'pool_name': 'pipeline_pool',
                'pool_size': pool_config.pool_size,
                'pool_reset_session': True,
                'host': db_config.host,
                'port': db_config.port,
                'user': db_config.user,
                'password': db_config.password,
                'database': db_config.database,
                'charset': db_config.charset,
                'autocommit': False,
                'time_zone': '+08:00'
            }
            
            self.pool = mysql.connector.pooling.MySQLConnectionPool(**pool_config_dict)
            logger.info(f"数据库连接池初始化成功，池大小: {pool_config.pool_size}")
            
        except Exception as e:
            logger.error(f"数据库连接池初始化失败: {e}")
            raise
    
    @contextmanager
    def get_connection(self):
        """获取数据库连接（上下文管理器）"""
        connection = None
        try:
            connection = self.pool.get_connection()
            logger.debug("获取数据库连接成功")
            yield connection
        except Exception as e:
            logger.error(f"数据库连接错误: {e}")
            if connection:
                connection.rollback()
            raise
        finally:
            if connection:
                connection.close()
                logger.debug("数据库连接已关闭")
    
    @contextmanager
    def get_cursor(self, dictionary=True):
        """获取数据库游标（上下文管理器）"""
        with self.get_connection() as connection:
            cursor = None
            try:
                cursor = connection.cursor(dictionary=dictionary)
                yield cursor, connection
            except Exception as e:
                logger.error(f"数据库游标错误: {e}")
                connection.rollback()
                raise
            finally:
                if cursor:
                    cursor.close()
    
    def execute_query(self, sql: str, params: Optional[Tuple] = None) -> List[Dict[str, Any]]:
        """执行查询语句"""
        try:
            with self.get_cursor() as (cursor, connection):
                cursor.execute(sql, params or ())
                results = cursor.fetchall()
                logger.debug(f"查询执行成功，返回 {len(results)} 条记录")
                return results
        except Exception as e:
            logger.error(f"查询执行失败: {sql}, 错误: {e}")
            raise
    
    def execute_update(self, sql: str, params: Optional[Tuple] = None) -> int:
        """执行更新语句"""
        try:
            with self.get_cursor() as (cursor, connection):
                cursor.execute(sql, params or ())
                connection.commit()
                affected_rows = cursor.rowcount
                logger.debug(f"更新执行成功，影响 {affected_rows} 行")
                return affected_rows
        except Exception as e:
            logger.error(f"更新执行失败: {sql}, 错误: {e}")
            raise
    
    def execute_batch(self, sql: str, params_list: List[Tuple]) -> int:
        """批量执行语句"""
        try:
            with self.get_cursor() as (cursor, connection):
                cursor.executemany(sql, params_list)
                connection.commit()
                affected_rows = cursor.rowcount
                logger.info(f"批量执行成功，影响 {affected_rows} 行")
                return affected_rows
        except Exception as e:
            logger.error(f"批量执行失败: {sql}, 错误: {e}")
            raise
    
    def insert_dataframe(self, df: pd.DataFrame, table_name: str, 
                        if_exists: str = 'append', chunk_size: int = 1000) -> int:
        """将DataFrame插入到数据库表"""
        try:
            db_config = config.database
            
            # 使用PyMySQL连接（pandas to_sql需要）
            connection_string = (
                f"mysql+pymysql://{db_config.user}:{db_config.password}@"
                f"{db_config.host}:{db_config.port}/{db_config.database}"
                f"?charset={db_config.charset}"
            )
            
            rows_inserted = len(df)
            df.to_sql(
                name=table_name,
                con=connection_string,
                if_exists=if_exists,
                index=False,
                chunksize=chunk_size
            )
            
            logger.info(f"DataFrame插入成功，表: {table_name}, 行数: {rows_inserted}")
            return rows_inserted
            
        except Exception as e:
            logger.error(f"DataFrame插入失败: {table_name}, 错误: {e}")
            raise
    
    def get_table_info(self, table_name: str) -> List[Dict[str, Any]]:
        """获取表结构信息"""
        sql = f"DESCRIBE {table_name}"
        return self.execute_query(sql)
    
    def get_table_count(self, table_name: str, where_clause: str = "") -> int:
        """获取表记录数"""
        sql = f"SELECT COUNT(*) as count FROM {table_name}"
        if where_clause:
            sql += f" WHERE {where_clause}"
        
        result = self.execute_query(sql)
        return result[0]['count'] if result else 0
    
    def table_exists(self, table_name: str) -> bool:
        """检查表是否存在"""
        sql = """
        SELECT COUNT(*) as count 
        FROM information_schema.tables 
        WHERE table_schema = %s AND table_name = %s
        """
        db_config = config.database
        result = self.execute_query(sql, (db_config.database, table_name))
        return result[0]['count'] > 0 if result else False
    
    def get_tables(self) -> List[str]:
        """获取所有表名"""
        sql = """
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = %s
        """
        db_config = config.database
        results = self.execute_query(sql, (db_config.database,))
        # 处理不同的字段名格式
        table_names = []
        for row in results:
            if 'table_name' in row:
                table_names.append(row['table_name'])
            elif 'TABLE_NAME' in row:
                table_names.append(row['TABLE_NAME'])
            else:
                # 如果都没有，取第一个值
                table_names.append(list(row.values())[0])
        return table_names
    
    def test_connection(self) -> bool:
        """测试数据库连接"""
        try:
            with self.get_connection() as connection:
                if connection.is_connected():
                    logger.info("数据库连接测试成功")
                    return True
                else:
                    logger.error("数据库连接测试失败")
                    return False
        except Exception as e:
            logger.error(f"数据库连接测试失败: {e}")
            return False
    
    def close_pool(self):
        """关闭连接池"""
        if self.pool:
            # MySQL Connector/Python的连接池没有显式的关闭方法
            # 连接会在程序结束时自动关闭
            logger.info("数据库连接池已标记为关闭")


# 全局数据库管理器实例
db_manager = DatabaseManager()