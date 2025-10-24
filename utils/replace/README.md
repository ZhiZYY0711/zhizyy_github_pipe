# 区域数据替换工具

## 功能说明

这个工具用于将 `area.sql` 文件中的 null 值替换为对应的区域编码、省、市、县信息。

## 文件说明

- `code.txt`: 包含区域编码和名称的数据文件
- `replace_area_data.py`: 主要的处理脚本
- `area_updated.sql`: 生成的更新后的SQL文件

## 使用方法

1. 确保 `code.txt` 和原始 `area.sql` 文件在正确的位置
2. 运行脚本：
   ```bash
   python replace_area_data.py
   ```
3. 查看生成的 `area_updated.sql` 文件

## 处理规则

- **省级数据** (编码以0000结尾): `province=省名, city='-1', district='-1'`
- **市级数据** (编码以00结尾): `province=省名, city=市名, district='-1'`
- **区县级数据**: `province=省名, city=市名, district=区县名`

## 处理结果

- 成功处理了 3209 条记录
- 省级: 34 个
- 市级: 333 个  
- 区县级: 2842 个

## 示例输出

```sql
-- 省级
insert into area (id, province, city, district, create_time, update_time) values ('110000', '北京市', '-1', '-1', '2010-05-19 19:17:54', '2001-12-07 18:11:30');

-- 市级
insert into area (id, province, city, district, create_time, update_time) values ('130100', '河北省', '石家庄市', '-1', '2009-11-08 05:07:14', '2001-03-18 19:04:26');

-- 区县级
insert into area (id, province, city, district, create_time, update_time) values ('130102', '河北省', '石家庄市', '长安区', '2016-09-03 15:21:22', '2000-06-24 20:29:15');
```