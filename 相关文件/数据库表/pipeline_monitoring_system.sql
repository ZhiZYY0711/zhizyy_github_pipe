
-- 管道监测系统数据库表结构

-- 检修员登录表
CREATE TABLE repairman_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检修员的唯一标识',
    username VARCHAR(20) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码哈希值（增强安全性）',
    last_login_time DATETIME DEFAULT NOW() COMMENT '上次登录时间',
    password_updated_at DATETIME DEFAULT NOW() COMMENT '上次修改密码时间',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 管理员登录表
CREATE TABLE admin_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员的唯一标识',
    username VARCHAR(20) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码哈希值（增强安全性）',
    last_login_time DATETIME DEFAULT NOW() COMMENT '上次登录时间',
    password_updated_at DATETIME DEFAULT NOW() COMMENT '上次修改密码时间',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 监测数据表
CREATE TABLE inspection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '监测数据的唯一标识',
    pressure DECIMAL(10,2) NOT NULL COMMENT '压力值',
    temperature DECIMAL(10,2) NOT NULL COMMENT '温度',
    traffic DECIMAL(10,2) NOT NULL COMMENT '流量',
    shake DECIMAL(10,2) NOT NULL COMMENT '震动值',
    data_status TINYINT NOT NULL COMMENT '当前数据的状态 0 安全 1 良好 2 危险 3 高危',
    realtime_picture TEXT COMMENT '实时图片',
    sensor_id BIGINT NOT NULL COMMENT '传感器的id',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 传感器表
CREATE TABLE sensor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '传感器的唯一标识',
    area_id BIGINT NOT NULL COMMENT '传感器所属的区域的id',
    pipeline_id BIGINT NOT NULL COMMENT '传感器所属的管道的id',
    repairman_id BIGINT NOT NULL COMMENT '负责的检修员',
    status TINYINT NOT NULL COMMENT '当前传感器的状态 0 正常 1 异常 2 离线',
    type TINYINT NOT NULL COMMENT '传感器类型',
    last_overhaul_time DATETIME COMMENT '上次检修时间',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 检修员表
CREATE TABLE repairman (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检修员的唯一标识',
    name VARCHAR(20) NOT NULL COMMENT '姓名',
    age TINYINT COMMENT '年龄',
    sex TINYINT COMMENT '性别 0 男 1 女 2 未知',
    phone CHAR(15) COMMENT '联系方式',
    email VARCHAR(100) COMMENT '邮箱',
    entry_time DATETIME NOT NULL COMMENT '入职时间',
    area_id BIGINT NOT NULL COMMENT '所属区域',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 任务表
CREATE TABLE task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务的唯一标识',
    inspection_id BIGINT COMMENT '出现危险的监测记录的id',
    repairman_id BIGINT NOT NULL COMMENT '被分配任务的检修员的id',
    area_id BIGINT NOT NULL COMMENT '任务所属区域',
    pipe_id BIGINT NOT NULL COMMENT '任务所属管道',
    task_name VARCHAR(50) NOT NULL COMMENT '任务名称',
    type TINYINT NOT NULL COMMENT '任务类型',
    priority TINYINT NOT NULL DEFAULT 0 COMMENT '任务优先级 0 紧急 1 高 2 中',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '任务结果 0 已发布 1 已接取 2已完成 3 异常',
    public_time DATETIME NOT NULL COMMENT '任务发布时间',
    response_time DATETIME COMMENT '任务响应时间',
    accomplish_time DATETIME COMMENT '任务完成时间',
    feedback_information TEXT COMMENT '检修员的反馈信息（扩展长度）',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 演习表
CREATE TABLE manoeuvre (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '演习记录的唯一标识',
    area_id BIGINT COMMENT '演练所属区域',
    start_time DATETIME NOT NULL COMMENT '演练开始时间',
    end_time DATETIME COMMENT '演练结束时间',
    status TINYINT NOT NULL COMMENT '演练状态 0 成功 1 失败 2 进行中',
    type TINYINT NOT NULL COMMENT '演练类型 0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训',
    details TEXT COMMENT '演练描述',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '演习创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '演习更新时间'
);

-- 区域表
CREATE TABLE area (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '区域的唯一标识',
    province VARCHAR(8) NOT NULL COMMENT '省份',
    city VARCHAR(15) NOT NULL COMMENT '城市',
    district VARCHAR(15) NOT NULL COMMENT '行政区',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 管道表
CREATE TABLE pipe (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管道名的唯一标识',
    start_area_id BIGINT NOT NULL COMMENT '管道的起点区域的id',
    end_area_id BIGINT NOT NULL COMMENT '管道的终点区域的id',
    name VARCHAR(50) UNIQUE NOT NULL COMMENT '管道名称',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 演习和检修员连接表
CREATE TABLE conn_manoeuvre_repairman (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '演习和检修员连接表的唯一标识',
    manoeuvre_id BIGINT NOT NULL COMMENT '演习id',
    repairman_id BIGINT NOT NULL COMMENT '检修员id'
);

-- 日志表
CREATE TABLE log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志的唯一标识',
    user_id BIGINT NOT NULL COMMENT '管理员或检修员的id',
    type TINYINT NOT NULL COMMENT '操作员的类型 0 管理员 1 检修员',
    operate VARCHAR(50) NOT NULL COMMENT '进行的操作',
    status TINYINT NOT NULL COMMENT '操作的状态 0 成功 1 失败 2 未知',
    ip_address VARCHAR(45) NOT NULL COMMENT '操作者的ip地址',
    details TEXT COMMENT '操作详情',
    period DATETIME NOT NULL COMMENT '持续时间',
    operation_time DATETIME NOT NULL COMMENT '操作时间',
    create_time DATETIME NOT NULL DEFAULT NOW() COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT NOW() COMMENT '更新时间'
);

-- 备注: 所有外键字段均是逻辑外键，不使用物理外键
-- 不建立索引和分表
