#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
检查code.txt文件中的数据行数
"""

def check_code_file(file_path):
    """检查代码文件"""
    total_lines = 0
    valid_lines = 0
    empty_lines = 0
    invalid_lines = []
    
    print(f"正在检查文件: {file_path}")
    print("=" * 50)
    
    with open(file_path, 'r', encoding='utf-8') as f:
        for line_num, line in enumerate(f, 1):
            total_lines += 1
            original_line = line
            line = line.strip()
            
            if not line:
                empty_lines += 1
                continue
                
            parts = line.split(' ', 1)
            if len(parts) == 2:
                code, name = parts
                valid_lines += 1
            else:
                invalid_lines.append((line_num, line, original_line.strip()))
    
    print(f"总行数: {total_lines}")
    print(f"有效数据行: {valid_lines}")
    print(f"空行: {empty_lines}")
    print(f"格式无效行: {len(invalid_lines)}")
    
    if invalid_lines:
        print("\n格式无效的行:")
        for line_num, line, original in invalid_lines:
            print(f"第{line_num}行: '{line}' (原始: '{original}')")
            print(f"  分割结果: {line.split(' ')}")
    
    print("=" * 50)
    
    return valid_lines

if __name__ == "__main__":
    valid_count = check_code_file("code.txt")
    print(f"\n爬虫应该处理的数据条数: {valid_count}")