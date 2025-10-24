# 管道监测管理系统 - 数据生成工具

这是一个用于管道监测管理系统的MySQL数据库连接和数据生成脚手架工具。

## 功能特性

- 🔗 **数据库连接管理**: 支持MySQL连接池，自动重连和错误处理
- 📊 **数据生成器**: 内置多种数据生成器，支持区域、管道、巡检等数据
- ⚙️ **配置管理**: 支持YAML配置文件和环境变量
- 📝 **日志系统**: 基于loguru的完整日志记录
- 🐍 **虚拟环境**: 完整的Python虚拟环境配置
- 🔧 **扩展性**: 易于扩展的模块化设计

## 项目结构

```
generateData/
├── config/                 # 配置文件目录
│   └── database.yaml      # 数据库配置文件
├── src/                   # 源代码目录
│   ├── __init__.py
│   ├── config.py          # 配置管理模块
│   ├── database.py        # 数据库连接管理
│   ├── logger.py          # 日志配置
│   └── generators/        # 数据生成器
│       ├── __init__.py
│       ├── base_generator.py      # 基础生成器
│       ├── area_generator.py      # 区域数据生成器
│       ├── pipeline_generator.py  # 管道数据生成器
│       └── inspection_generator.py # 巡检数据生成器
├── examples/              # 示例脚本
│   ├── generate_sample_data.py    # 示例数据生成
│   └── test_connection.py         # 连接测试
├── logs/                  # 日志文件目录（自动创建）
├── venv/                  # Python虚拟环境（运行脚本后创建）
├── requirements.txt       # Python依赖包
├── setup.py              # 安装配置
├── create_venv.bat       # 虚拟环境创建脚本
├── .env.example          # 环境变量示例
└── README.md             # 项目说明
```

## 快速开始

### 1. 环境准备

确保你的系统已安装：
- Python 3.8+
- MySQL 5.7+ 或 MySQL 8.0+

### 2. 创建虚拟环境

运行批处理脚本自动创建虚拟环境：

```bash
# Windows
create_venv.bat

# 或手动创建
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

### 3. 配置数据库

1. 复制环境变量示例文件：
```bash
copy .env.example .env
```

2. 编辑 `.env` 文件，配置你的数据库连接信息：
```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=pipeline_management
```

3. 或者直接编辑 `config/database.yaml` 文件。

### 4. 测试连接

```bash
# 激活虚拟环境
venv\Scripts\activate

# 测试数据库连接
python examples/test_connection.py
```

### 5. 生成示例数据

```bash
# 生成示例数据
python examples/generate_sample_data.py
```

## 使用指南

### 数据库连接

```python
from src.database import db_manager

# 测试连接
if db_manager.test_connection():
    print("连接成功")

# 执行查询
results = db_manager.execute_query("SELECT * FROM area LIMIT 10")

# 执行更新
affected_rows = db_manager.execute_update("UPDATE area SET status = %s WHERE area_code = %s", ('active', 123456))

# 批量插入
import pandas as pd
df = pd.DataFrame([...])
db_manager.insert_dataframe(df, 'area')
```

### 数据生成

```python
from src.generators import AreaGenerator, PipelineGenerator

# 创建区域数据生成器
area_gen = AreaGenerator()

# 生成单条记录
record = area_gen.generate_record()

# 生成并保存批量数据
area_gen.generate_and_save(count=100)

# 获取表记录数
count = area_gen.get_table_count()
```

### 自定义数据生成器

继承 `BaseGenerator` 类创建自定义生成器：

```python
from src.generators.base_generator import BaseGenerator

class CustomGenerator(BaseGenerator):
    def get_table_name(self) -> str:
        return 'custom_table'
    
    def generate_record(self) -> dict:
        return {
            'id': self.fake.random_int(min=1, max=999999),
            'name': self.fake.name(),
            'created_at': self.fake.date_time()
        }

# 使用自定义生成器
custom_gen = CustomGenerator()
custom_gen.generate_and_save(count=50)
```

## 配置说明

### 数据库配置

在 `config/database.yaml` 中配置数据库连接：

```yaml
database:
  primary:
    host: ${DB_HOST:localhost}
    port: ${DB_PORT:3306}
    user: ${DB_USER:root}
    password: ${DB_PASSWORD:}
    database: ${DB_NAME:pipeline_management}
    charset: utf8mb4
    
  pool:
    pool_size: ${DB_POOL_SIZE:10}
    max_overflow: ${DB_MAX_OVERFLOW:20}
    pool_timeout: ${DB_POOL_TIMEOUT:30}
```

### 环境变量

支持的环境变量：

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| DB_HOST | localhost | 数据库主机 |
| DB_PORT | 3306 | 数据库端口 |
| DB_USER | root | 数据库用户名 |
| DB_PASSWORD | | 数据库密码 |
| DB_NAME | pipeline_management | 数据库名 |
| DB_POOL_SIZE | 10 | 连接池大小 |
| LOG_LEVEL | INFO | 日志级别 |
| BATCH_SIZE | 1000 | 批处理大小 |

## 依赖包说明

| 包名 | 版本 | 用途 |
|------|------|------|
| PyMySQL | 1.1.0 | MySQL数据库连接 |
| mysql-connector-python | 8.2.0 | MySQL官方连接器 |
| pandas | 2.1.4 | 数据处理 |
| python-dotenv | 1.0.0 | 环境变量管理 |
| PyYAML | 6.0.1 | YAML配置文件解析 |
| loguru | 0.7.2 | 日志记录 |
| Faker | 20.1.0 | 测试数据生成 |
| pydantic | 2.5.2 | 数据验证 |

## 常见问题

### 1. 数据库连接失败

- 检查数据库服务是否启动
- 验证连接参数是否正确
- 确认数据库用户权限
- 检查防火墙设置

### 2. 表不存在错误

在生成数据前，请确保数据库中已创建相应的表结构。

### 3. 编码问题

确保数据库和连接都使用 UTF-8 编码：
```sql
CREATE DATABASE pipeline_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. 虚拟环境问题

如果虚拟环境创建失败，请手动执行：
```bash
python -m venv venv
venv\Scripts\activate
pip install --upgrade pip
pip install -r requirements.txt
```

## 开发指南

### 添加新的数据生成器

1. 在 `src/generators/` 目录下创建新的生成器文件
2. 继承 `BaseGenerator` 类
3. 实现 `get_table_name()` 和 `generate_record()` 方法
4. 在 `src/generators/__init__.py` 中导入新生成器

### 扩展配置

在 `config/database.yaml` 中添加新的配置项，并在 `src/config.py` 中添加相应的配置类。

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- 项目维护者: Pipeline Team
- 邮箱: team@pipeline.com