-- 管道管理系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS pipeline_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE pipeline_management;

-- 管理员表
CREATE TABLE IF NOT EXISTS manager (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '电话',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '管理员表';

-- 监控数据表
CREATE TABLE IF NOT EXISTS monitoring_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sensor_id BIGINT NOT NULL COMMENT '传感器ID',
    sensor_type VARCHAR(50) NOT NULL COMMENT '传感器类型',
    location VARCHAR(200) NOT NULL COMMENT '位置',
    value DECIMAL(10,2) NOT NULL COMMENT '监测值',
    unit VARCHAR(20) NOT NULL COMMENT '单位',
    status TINYINT DEFAULT 1 COMMENT '状态：0-异常，1-正常',
    threshold_min DECIMAL(10,2) COMMENT '阈值下限',
    threshold_max DECIMAL(10,2) COMMENT '阈值上限',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sensor_id (sensor_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '监控数据表';

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    type VARCHAR(50) NOT NULL COMMENT '任务类型',
    priority TINYINT DEFAULT 1 COMMENT '优先级：1-低，2-中，3-高',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待处理，1-进行中，2-已完成，3-已取消',
    assigned_to BIGINT COMMENT '分配给（维修工ID）',
    location VARCHAR(200) COMMENT '任务位置',
    estimated_duration INT COMMENT '预计耗时（分钟）',
    actual_duration INT COMMENT '实际耗时（分钟）',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    deadline DATETIME COMMENT '截止时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_deadline (deadline)
) COMMENT '任务表';

-- 维修工表
CREATE TABLE IF NOT EXISTS repairman (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) NOT NULL COMMENT '电话',
    email VARCHAR(100) COMMENT '邮箱',
    skill_level TINYINT DEFAULT 1 COMMENT '技能等级：1-初级，2-中级，3-高级',
    specialization VARCHAR(100) COMMENT '专业领域',
    status TINYINT DEFAULT 1 COMMENT '状态：0-离职，1-在职，2-休假',
    hire_date DATE COMMENT '入职日期',
    total_tasks INT DEFAULT 0 COMMENT '总任务数',
    completed_tasks INT DEFAULT 0 COMMENT '已完成任务数',
    average_rating DECIMAL(3,2) DEFAULT 0.00 COMMENT '平均评分',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_skill_level (skill_level)
) COMMENT '维修工表';

-- 日志表
CREATE TABLE IF NOT EXISTS log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) NOT NULL COMMENT '操作',
    method VARCHAR(10) COMMENT '请求方法',
    url VARCHAR(500) COMMENT '请求URL',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    type TINYINT DEFAULT 1 COMMENT '日志类型：1-操作日志，2-登录日志，3-错误日志',
    status TINYINT DEFAULT 1 COMMENT '状态：0-失败，1-成功',
    error_message TEXT COMMENT '错误信息',
    execution_time BIGINT COMMENT '执行时间（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) COMMENT '日志表';

-- 插入初始管理员数据
INSERT INTO manager (username, password, name, email, phone) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKyVqNhKwvKfkJ5aYjKABqST5.Fy', '系统管理员', 'admin@example.com', '13800138000');
-- 密码为：admin123

-- 插入示例监控数据
INSERT INTO monitoring_data (sensor_id, sensor_type, location, value, unit, status, threshold_min, threshold_max) VALUES
(1001, '压力传感器', '管道A区段1', 2.5, 'MPa', 1, 1.0, 5.0),
(1002, '温度传感器', '管道A区段2', 25.3, '℃', 1, -10.0, 80.0),
(1003, '流量传感器', '管道B区段1', 150.2, 'L/min', 1, 50.0, 300.0),
(1004, '压力传感器', '管道B区段2', 6.8, 'MPa', 0, 1.0, 5.0),
(1005, '温度传感器', '管道C区段1', 85.5, '℃', 0, -10.0, 80.0);

-- 插入示例维修工数据
INSERT INTO repairman (name, phone, email, skill_level, specialization, status, hire_date, total_tasks, completed_tasks, average_rating) VALUES
('张三', '13800138001', 'zhangsan@example.com', 3, '管道维修', 1, '2023-01-15', 45, 42, 4.5),
('李四', '13800138002', 'lisi@example.com', 2, '设备检修', 1, '2023-03-20', 32, 30, 4.2),
('王五', '13800138003', 'wangwu@example.com', 2, '电气维修', 1, '2023-05-10', 28, 25, 4.0),
('赵六', '13800138004', 'zhaoliu@example.com', 1, '基础维护', 1, '2023-08-01', 15, 14, 3.8);

-- 插入示例任务数据
INSERT INTO task (title, description, type, priority, status, assigned_to, location, estimated_duration, actual_duration, start_time, end_time, deadline) VALUES
('管道A区段1压力检修', '压力传感器显示异常，需要检查管道压力系统', '维修', 3, 2, 1, '管道A区段1', 120, 110, '2024-01-15 09:00:00', '2024-01-15 10:50:00', '2024-01-15 18:00:00'),
('管道B区段2压力维修', '压力超出正常范围，需要紧急处理', '维修', 3, 1, 1, '管道B区段2', 180, NULL, '2024-01-16 08:00:00', NULL, '2024-01-16 20:00:00'),
('管道C区段1温度检查', '温度传感器报警，需要检查温度控制系统', '检查', 2, 0, NULL, '管道C区段1', 90, NULL, NULL, NULL, '2024-01-17 16:00:00'),
('设备定期保养', '对所有监控设备进行定期保养', '保养', 1, 0, 2, '全区域', 480, NULL, NULL, NULL, '2024-01-20 18:00:00');

-- 插入示例日志数据
INSERT INTO log (user_id, username, operation, method, url, ip_address, type, status, execution_time) VALUES
(1, 'admin', '管理员登录', 'POST', '/domain/manager/login', '192.168.1.100', 2, 1, 150),
(1, 'admin', '查询监控数据', 'GET', '/monitoring-data/page', '192.168.1.100', 1, 1, 80),
(1, 'admin', '创建任务', 'POST', '/task', '192.168.1.100', 1, 1, 200),
(1, 'admin', '更新维修工信息', 'PUT', '/repairman/1', '192.168.1.100', 1, 1, 120);