#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
区域数据替换脚本
将area.sql文件中的null值替换为对应的区域编码、省、市、县信息
"""

import re
import os
from typing import Dict, Tuple, Optional


class AreaDataProcessor:
    def __init__(self):
        self.area_data = {}  # 存储区域编码和名称的映射
        self.province_map = {}  # 省级映射
        self.city_map = {}  # 市级映射
        self.district_map = {}  # 区县级映射
        
    def load_area_codes(self, code_file_path: str):
        """加载区域编码数据"""
        print(f"正在加载区域编码数据: {code_file_path}")
        
        with open(code_file_path, 'r', encoding='utf-8') as f:
            for line_num, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                    
                # 解析格式: "编码 名称"
                parts = line.split(' ', 1)
                if len(parts) != 2:
                    print(f"警告: 第{line_num}行格式不正确: {line}")
                    continue
                    
                code, name = parts
                if not code.isdigit() or len(code) != 6:
                    print(f"警告: 第{line_num}行编码格式不正确: {code}")
                    continue
                    
                self.area_data[code] = name
                
                # 根据编码规则分类存储
                if code.endswith('0000'):  # 省级
                    self.province_map[code] = name
                elif code.endswith('00'):  # 市级
                    self.city_map[code] = name
                else:  # 区县级
                    self.district_map[code] = name
                    
        print(f"加载完成: 省级{len(self.province_map)}个, 市级{len(self.city_map)}个, 区县级{len(self.district_map)}个")
        
    def get_area_info(self, code: str) -> Tuple[str, Optional[str], Optional[str], Optional[str]]:
        """
        根据区域编码获取省市区信息
        返回: (编码, 省名, 市名, 区县名)
        """
        if code not in self.area_data:
            return code, None, None, None
            
        area_name = self.area_data[code]
        
        # 判断级别
        if code.endswith('0000'):  # 省级
            return code, area_name, '-1', '-1'
        elif code.endswith('00'):  # 市级
            province_code = code[:2] + '0000'
            province_name = self.province_map.get(province_code, '-1')
            return code, province_name, area_name, '-1'
        else:  # 区县级
            province_code = code[:2] + '0000'
            city_code = code[:4] + '00'
            province_name = self.province_map.get(province_code, '-1')
            city_name = self.city_map.get(city_code, '-1')
            return code, province_name, city_name, area_name
            
    def process_sql_file(self, sql_file_path: str, output_file_path: str):
        """处理SQL文件，替换null值"""
        print(f"正在处理SQL文件: {sql_file_path}")
        
        # SQL语句的正则表达式模式
        pattern = r"insert into area \(id, province, city, district, create_time, update_time\) values \(null, null, null, null, '([^']+)', '([^']+)'\);"
        
        processed_count = 0
        
        with open(sql_file_path, 'r', encoding='utf-8') as input_file, \
             open(output_file_path, 'w', encoding='utf-8') as output_file:
            
            area_codes = list(self.area_data.keys())
            
            for line_num, line in enumerate(input_file, 1):
                line = line.strip()
                if not line:
                    continue
                    
                match = re.match(pattern, line)
                if match:
                    create_time, update_time = match.groups()
                    
                    # 使用对应的区域编码
                    if processed_count < len(area_codes):
                        code = area_codes[processed_count]
                        area_id, province, city, district = self.get_area_info(code)
                        
                        # 构建新的SQL语句
                        new_sql = (f"insert into area (id, province, city, district, create_time, update_time) "
                                 f"values ('{area_id}', '{province}', '{city}', '{district}', "
                                 f"'{create_time}', '{update_time}');")
                        
                        output_file.write(new_sql + '\n')
                        processed_count += 1
                    else:
                        # 如果区域编码用完了，保持原样
                        output_file.write(line + '\n')
                else:
                    # 不匹配的行保持原样
                    output_file.write(line + '\n')
                    
        print(f"处理完成: 共替换了{processed_count}条记录")
        print(f"输出文件: {output_file_path}")


def main():
    """主函数"""
    # 文件路径
    base_dir = os.path.dirname(os.path.abspath(__file__))
    code_file = os.path.join(base_dir, 'code.txt')
    sql_file = os.path.join(base_dir, '..', '..', '相关文件', '数据库表', 'area.sql')
    output_file = os.path.join(base_dir, 'area_updated.sql')
    
    # 检查文件是否存在
    if not os.path.exists(code_file):
        print(f"错误: 找不到区域编码文件: {code_file}")
        return
        
    if not os.path.exists(sql_file):
        print(f"错误: 找不到SQL文件: {sql_file}")
        return
    
    # 创建处理器并执行
    processor = AreaDataProcessor()
    
    try:
        # 加载区域编码数据
        processor.load_area_codes(code_file)
        
        # 处理SQL文件
        processor.process_sql_file(sql_file, output_file)
        
        print("\n✅ 处理完成!")
        print(f"📁 输出文件: {output_file}")
        
    except Exception as e:
        print(f"❌ 处理过程中出现错误: {e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    main()