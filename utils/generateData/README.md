# 管道监测系统数据生成工具

这是一个用于生成管道监测系统测试数据的统一工具，支持生成管道、传感器、巡检和检修员登录等多种类型的数据。

## 功能特性

- 🚀 **统一主程序**: 一个程序支持所有数据类型的生成
- 🎯 **命令行支持**: 支持命令行参数和交互式操作
- 📊 **多种数据类型**: 支持管道、传感器、巡检、检修员登录数据生成
- 🔧 **灵活配置**: 支持自定义配置文件
- 📈 **实时状态**: 显示数据库表状态和记录数量
- 🛡️ **错误处理**: 完善的错误处理和日志记录

## 支持的数据类型

| 数据类型 | 命令行参数 | 描述 |
|---------|-----------|------|
| 管道数据 | `pipeline` | 生成管道基础信息数据 |
| 传感器数据 | `sensor` | 生成传感器设备数据 |
| 巡检数据 | `inspection` | 生成巡检监测数据 |
| 检修员登录数据 | `repairman_registration` | 生成检修员登录记录数据 |
| 所有数据 | `all` | 生成所有类型的数据 |

## 安装和配置

### 1. 环境准备

```bash
# 创建虚拟环境
python -m venv venv

# 激活虚拟环境 (Windows)
venv\Scripts\activate

# 激活虚拟环境 (Linux/Mac)
source venv/bin/activate

# 安装依赖
pip install -r requirements.txt
```

### 2. 数据库配置

复制 `.env.example` 为 `.env` 并配置数据库连接信息：

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```env
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_USER=your_username
DB_PASSWORD=your_password
DB_NAME=pipeline_monitoring_system

# 日志配置
LOG_LEVEL=INFO
LOG_FILE=logs/app.log
```

## 使用方法

### 命令行模式

#### 基本用法

```bash
# 生成100条管道数据
python main.py --type pipeline --count 100

# 生成50条传感器数据
python main.py --type sensor --count 50

# 生成30条巡检数据
python main.py --type inspection --count 30

# 生成20条检修员登录数据
python main.py --type repairman_registration --count 20

# 生成所有类型的数据，每种10条
python main.py --type all --count 10
```

#### 高级选项

```bash
# 指定处理已存在表的方式
python main.py --type pipeline --count 100 --if-exists replace

# 使用自定义配置文件
python main.py --type sensor --count 50 --config custom_config.yaml

# 设置日志级别
python main.py --type inspection --count 30 --log-level DEBUG

# 显示配置文件模板
python main.py --template
```

#### 参数说明

- `--type, -t`: 指定生成数据类型 (pipeline/sensor/inspection/repairman_registration/all)
- `--count, -c`: 生成记录数量 (默认: 100)
- `--if-exists`: 如果表存在的处理方式 (append/replace/fail, 默认: append)
- `--config`: 自定义配置文件路径
- `--template`: 显示配置文件模板
- `--log-level`: 日志级别 (DEBUG/INFO/WARNING/ERROR, 默认: INFO)
- `--help, -h`: 显示帮助信息

### 交互式模式

直接运行程序进入交互式模式：

```bash
python main.py
```

交互式模式提供：
- 📋 数据类型选择菜单
- 📊 数据库表状态显示
- ⚙️ 生成选项配置
- 🔄 批量数据生成

## 自定义配置

### 配置文件格式

创建 YAML 格式的配置文件来自定义数据生成规则：

```yaml
# 管道数据配置
pipeline:
  diameter_range: [100, 1000]  # 管径范围(mm)
  length_range: [1000, 50000]  # 长度范围(m)
  materials: ["钢管", "塑料管", "复合管"]
  
# 传感器数据配置  
sensor:
  types: [0, 1, 2, 3, 4]  # 传感器类型
  status: [0, 1, 2]       # 传感器状态
  
# 巡检数据配置
inspection:
  result_types: [0, 1, 2]  # 巡检结果类型
  
# 检修员登录数据配置
repairman_registration:
  username_patterns: ["repair_", "tech_", "maint_"]
  password_complexity: "medium"  # 密码复杂度
```

### 使用自定义配置

```bash
python main.py --type pipeline --count 100 --config my_config.yaml
```

## 数据库表结构

### 管道表 (pipe)
- `id`: 主键
- `name`: 管道名称
- `diameter`: 管径
- `length`: 长度
- `material`: 材质
- `start_area_id`: 起始区域ID
- `end_area_id`: 结束区域ID
- `create_time`: 创建时间
- `update_time`: 更新时间

### 传感器表 (sensor)
- `id`: 主键
- `area_id`: 区域ID
- `pipeline_id`: 管道ID
- `repairman_id`: 检修员ID
- `status`: 状态
- `type`: 类型
- `last_overhaul_time`: 最后检修时间
- `create_time`: 创建时间
- `update_time`: 更新时间

### 巡检表 (inspection)
- `id`: 主键
- `sensor_id`: 传感器ID
- `result`: 巡检结果
- `create_time`: 创建时间
- `update_time`: 更新时间

### 检修员登录表 (repairman_registration)
- `id`: 主键
- `username`: 用户名
- `password_hash`: 密码哈希
- `last_login_time`: 最后登录时间
- `password_update_time`: 密码更新时间
- `create_time`: 创建时间
- `update_time`: 更新时间

## 日志和监控

程序提供详细的日志记录：

- **INFO**: 正常操作信息
- **WARNING**: 警告信息
- **ERROR**: 错误信息
- **DEBUG**: 调试信息

日志文件位置：`logs/app.log`

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查 `.env` 文件中的数据库配置
   - 确认数据库服务正在运行
   - 验证用户名和密码

2. **表不存在错误**
   - 确保数据库中已创建相应的表
   - 运行数据库初始化脚本

3. **字段不匹配错误**
   - 检查数据库表结构是否与生成器匹配
   - 更新表结构或修改生成器配置

4. **依赖包缺失**
   ```bash
   pip install -r requirements.txt
   ```

### 调试模式

使用调试模式获取详细信息：

```bash
python main.py --type pipeline --count 10 --log-level DEBUG
```

## 开发说明

### 项目结构

```
generateData/
├── main.py                 # 主程序入口
├── requirements.txt        # 依赖包列表
├── .env.example           # 环境变量模板
├── README.md              # 项目说明
├── src/                   # 源代码目录
│   ├── generators/        # 数据生成器
│   │   ├── __init__.py
│   │   ├── base_generator.py
│   │   ├── pipeline_generator.py
│   │   ├── sensor_generator.py
│   │   ├── inspection_generator.py
│   │   └── repairman_registration_generator.py
│   ├── config.py          # 配置管理
│   ├── database.py        # 数据库操作
│   └── logger.py          # 日志配置
└── logs/                  # 日志文件目录
```

### 扩展新的数据生成器

1. 继承 `BaseGenerator` 类
2. 实现 `get_table_name()` 和 `generate_record()` 方法
3. 在 `main.py` 中注册新的生成器
4. 更新命令行参数选项

## 许可证

本项目采用 MIT 许可证。

## 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

---

**注意**: 请确保在生产环境中使用前充分测试，并根据实际需求调整数据生成规则。