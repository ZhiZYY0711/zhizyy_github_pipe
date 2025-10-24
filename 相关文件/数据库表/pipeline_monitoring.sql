
-- 删除数据库（如果存在）
DROP DATABASE IF EXISTS pipeline_monitoring;

-- 创建数据库
CREATE DATABASE pipeline_monitoring CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE pipeline_monitoring;

-- 登陆表
CREATE TABLE registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检修员或管理员的唯一标识',
    type TINYINT NOT NULL COMMENT '类型 0 管理员 1 检修员',
    username VARCHAR(20) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码哈希值（增强安全性）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    phone CHAR(11) NULL COMMENT '手机号',
    email VARCHAR(100) NULL COMMENT '邮箱',
    last_login_time DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上次登录时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登陆表';

-- 监测数据表
CREATE TABLE inspection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '监测数据的唯一标识',
    pressure DECIMAL(10,2) NOT NULL COMMENT '压力值',
    temperature DECIMAL(10,2) NOT NULL COMMENT '温度',
    traffic DECIMAL(10,2) NOT NULL COMMENT '流量',
    shake DECIMAL(10,2) NOT NULL COMMENT '震动值',
    data_status TINYINT NOT NULL COMMENT '当前数据的状态 0 安全 1 良好 2 危险 3 高危',
    realtime_picture VARCHAR(300) NULL COMMENT '实时图片',
    sensor_id BIGINT COMMENT '传感器的id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测数据表';

-- 传感器表
CREATE TABLE sensor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '传感器的唯一标识',
    area_id BIGINT COMMENT '传感器所属的区域的id',
    pipeline_id BIGINT COMMENT '传感器所属的管道的id',
    repairman_id BIGINT COMMENT '负责的检修员',
    status TINYINT NOT NULL COMMENT '当前传感器的状态 0 正常 1 异常 2 离线',
    type TINYINT NOT NULL COMMENT '传感器类型',
    last_overhaul_time DATETIME NULL COMMENT '上次检修时间',
    position VARCHAR(30) NOT NULL COMMENT '传感器的经纬度位置',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传感器表';

-- 检修员表
CREATE TABLE repairman (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检修员的唯一标识',
    name VARCHAR(5) NOT NULL COMMENT '姓名',
    age TINYINT NOT NULL COMMENT '年龄',
    sex TINYINT NOT NULL COMMENT '性别',
    phone CHAR(11) NOT NULL COMMENT '联系方式',
    entry_time DATETIME NOT NULL COMMENT '入职时间',
    area_id BIGINT COMMENT '所属区域',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检修员表';

-- 任务表
CREATE TABLE task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务的唯一标识',
    inspection_id BIGINT COMMENT '出现危险的监测记录的id',
    repairman_id BIGINT COMMENT '被分配任务的检修员的id',
    area_id BIGINT COMMENT '任务所属区域',
    pipe_id BIGINT COMMENT '任务所属管道',
    task_name VARCHAR(10) NOT NULL COMMENT '任务名称',
    type TINYINT NOT NULL COMMENT '任务类型',
    priority TINYINT NOT NULL DEFAULT 0 COMMENT '任务优先级 0 紧急 1 高 2 中',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '任务结果 0 已发布 1 已接取 2已完成 3 异常',
    public_time DATETIME NOT NULL COMMENT '任务发布时间',
    response_time DATETIME NULL COMMENT '任务响应时间',
    accomplish_time DATETIME NULL COMMENT '任务完成时间',
    feedback_information TEXT COMMENT '检修员的反馈信息（扩展长度）',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 演习表
CREATE TABLE manoeuvre (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '演习记录的唯一标识',
    area_id BIGINT COMMENT '演练所属区域',
    start_time DATETIME NOT NULL COMMENT '演练开始时间',
    end_time DATETIME NOT NULL COMMENT '演练结束时间',
    status TINYINT NOT NULL COMMENT '演练状态 0 成功 1 失败 2 进行中',
    type TINYINT NOT NULL COMMENT '演练类型 0 状态模拟 1 事故模拟 2 紧急事故 3 日常作训',
    details TEXT NULL COMMENT '演练描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '演习创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '演习更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演习表';

-- 区域表
CREATE TABLE area (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '区域的唯一标识',
    province VARCHAR(8) NOT NULL COMMENT '省份',
    city VARCHAR(15) NOT NULL COMMENT '城市',
    district VARCHAR(15) NOT NULL COMMENT '行政区',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='区域表';

-- 管道表
CREATE TABLE pipe (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管道名的唯一标识',
    start BIGINT COMMENT '管道的起点区域的id',
    end BIGINT COMMENT '管道的终点区域的id',
    name VARCHAR(10) UNIQUE NOT NULL COMMENT '管道名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管道表';

-- 演习和检修员连接表
CREATE TABLE manoeuvre_connect_repairman (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '演习和检修员连接表的唯一标识',
    manoeuvre_id BIGINT COMMENT '演习id',
    repairman_id BIGINT COMMENT '检修员id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演习和检修员连接表';

-- 日志表
CREATE TABLE log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志的唯一标识',
    user_id BIGINT COMMENT '管理员或检修员的id',
    operate VARCHAR(20) NOT NULL COMMENT '进行的操作',
    status TINYINT NOT NULL COMMENT '操作的状态',
    grade TINYINT NOT NULL COMMENT '操作级别',
    ip_address VARCHAR(15) NOT NULL COMMENT '操作者的ip地址',
    details TEXT NULL COMMENT '操作详情',
    period DATETIME NOT NULL COMMENT '持续时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日志表';
