-- 油气管道监测管理系统数据库

-- 创建数据库
CREATE DATABASE IF NOT EXISTS pipeline_management_system;
USE pipeline_management_system;

-- 1. registration 登录表
CREATE TABLE registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '检修员或管理员的唯一标识',
    type TINYINT NOT NULL COMMENT '用户类型 0 全局管理员 1 区域管理员 2 检修员',
    username VARCHAR(20) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码哈希值（增强安全性）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 2. area 区域表
CREATE TABLE area (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '区域的唯一标识',
    province VARCHAR(8) COMMENT '省份',
    city VARCHAR(15) COMMENT '城市',
    district VARCHAR(15) COMMENT '行政区',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 3. pipe 管道表
CREATE TABLE pipe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管道名的唯一标识',
    start BIGINT NOT NULL COMMENT '管道的起点区域的id',
    end BIGINT NOT NULL COMMENT '管道的终点区域的id',
    name VARCHAR(10) UNIQUE NOT NULL COMMENT '管道名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (start) REFERENCES area(id),
    FOREIGN KEY (end) REFERENCES area(id)
);

-- 4. sensor 传感器表
CREATE TABLE sensor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '传感器的唯一标识',
    area_id BIGINT NOT NULL COMMENT '传感器所属的区域的id',
    pipeline_id BIGINT NOT NULL COMMENT '传感器所属的管道的id',
    status TINYINT NOT NULL COMMENT '当前传感器的状态 0 正常 1 异常 2 离线',
    position VARCHAR(30) NOT NULL COMMENT '传感器的经纬度位置',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (area_id) REFERENCES area(id),
    FOREIGN KEY (pipeline_id) REFERENCES pipe(id)
);

-- 5. repairman 检修员表
CREATE TABLE repairman (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '检修员的唯一标识',
    name VARCHAR(5) NOT NULL COMMENT '姓名',
    age TINYINT NOT NULL COMMENT '年龄',
    sex TINYINT NOT NULL COMMENT '性别',
    phone CHAR(11) NOT NULL COMMENT '联系方式',
    entry_time DATETIME NOT NULL COMMENT '入职时间',
    area_id BIGINT NOT NULL COMMENT '所属区域',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (area_id) REFERENCES area(id)
);

-- 6. inspection 监测数据表
CREATE TABLE inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '监测数据的唯一标识',
    pressure DECIMAL(10,2) NOT NULL COMMENT '压力值',
    temperature DECIMAL(10,2) NOT NULL COMMENT '温度',
    traffic DECIMAL(10,2) NOT NULL COMMENT '流量',
    shake DECIMAL(10,2) NOT NULL COMMENT '震动值',
    data_status TINYINT NOT NULL COMMENT '当前数据的状态 0 安全 1 良好 2 危险 3 高危',
    realtime_picture VARCHAR(300) COMMENT '实时图片',
    sensor_id BIGINT NOT NULL COMMENT '传感器的id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (sensor_id) REFERENCES sensor(id)
);

-- 7. task 任务表
CREATE TABLE task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务的唯一标识',
    inspection_id BIGINT NOT NULL COMMENT '出现危险的监测记录的id',
    repairman_id BIGINT NOT NULL COMMENT '被分配任务的检修员的id',
    result TINYINT NOT NULL DEFAULT 0 COMMENT '任务结果 0 已发布 1 已接取 2已完成 3 异常',
    public_time DATETIME NOT NULL COMMENT '任务发布时间',
    response_time DATETIME NULL COMMENT '任务响应时间',
    accomplish_time DATETIME NULL COMMENT '任务完成时间',
    feedback_information TEXT COMMENT '检修员的反馈信息（扩展长度）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (inspection_id) REFERENCES inspection(id),
    FOREIGN KEY (repairman_id) REFERENCES repairman(id)
);

-- 8. journal_inspection 监测数据日志表
CREATE TABLE journal_inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '监测数据的日志的唯一标识',
    inspection_id BIGINT NOT NULL COMMENT '监测数据的id',
    pressure DECIMAL(10,2) NOT NULL COMMENT '压力值',
    temperature DECIMAL(10,2) NOT NULL COMMENT '温度',
    traffic DECIMAL(10,2) NOT NULL COMMENT '流量',
    shake DECIMAL(10,2) NOT NULL COMMENT '震动值',
    data_status TINYINT NOT NULL COMMENT '数据的状态 0 安全 1 良好 2 危险 3 高危',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (inspection_id) REFERENCES inspection(id)
);

-- 9. journal_sensor 传感器日志表
CREATE TABLE journal_sensor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '传感器的日志的唯一标识',
    sensor_id BIGINT NOT NULL COMMENT '传感器的id',
    status TINYINT NOT NULL COMMENT '当前传感器的状态 0 正常 1 异常 2 离线',
    overhaul_time DATETIME NULL COMMENT '传感器检修的时间',
    repairman_id BIGINT NULL COMMENT '上次检修的检修员的id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (sensor_id) REFERENCES sensor(id),
    FOREIGN KEY (repairman_id) REFERENCES repairman(id)
);

-- 创建索引以提高查询性能
CREATE INDEX idx_sensor_area ON sensor(area_id);
CREATE INDEX idx_sensor_pipeline ON sensor(pipeline_id);
CREATE INDEX idx_inspection_sensor ON inspection(sensor_id);
CREATE INDEX idx_task_inspection ON task(inspection_id);
CREATE INDEX idx_task_repairman ON task(repairman_id);
CREATE INDEX idx_journal_inspection_id ON journal_inspection(inspection_id);
CREATE INDEX idx_journal_sensor_id ON journal_sensor(sensor_id);
CREATE INDEX idx_repairman_area ON repairman(area_id);