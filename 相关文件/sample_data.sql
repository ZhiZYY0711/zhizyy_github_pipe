USE pipeline_management_system;

-- 1. registration 登录表
INSERT INTO registration (type, username, password) VALUES
(0, 'admin_global', 'hashed_password_global'),
(1, 'admin_area_shanghai', 'hashed_password_shanghai'),
(2, 'repairman_zhangsan', 'hashed_password_zhangsan'),
(2, 'repairman_lisi', 'hashed_password_lisi');

-- 2. area 区域表
INSERT INTO area (province, city, district) VALUES
('上海市', '上海市', '浦东新区'),
('上海市', '上海市', '闵行区'),
('江苏省', '南京市', '玄武区'),
('浙江省', '杭州市', '西湖区');

-- 3. pipe 管道表
-- 假设 area 表的 id 从 1 开始自增
INSERT INTO pipe (start, end, name) VALUES
(1, 2, '沪闵管道'),
(3, 4, '宁杭管道');

-- 4. sensor 传感器表
-- 假设 area 表的 id 从 1 开始自增, pipe 表的 id 从 1 开始自增
INSERT INTO sensor (area_id, pipeline_id, status, position) VALUES
(1, 1, 0, '31.2304,121.4737'), -- 上海浦东
(2, 1, 0, '31.1304,121.3737'), -- 上海闵行
(3, 2, 1, '32.0603,118.7965'), -- 南京玄武 (异常)
(4, 2, 0, '30.2741,120.1551'); -- 杭州西湖

-- 5. repairman 检修员表
-- 假设 area 表的 id 从 1 开始自增
INSERT INTO repairman (name, age, sex, phone, entry_time, area_id) VALUES
('张三', 30, 0, '13800000001', '2022-01-01 09:00:00', 1),
('李四', 28, 0, '13800000002', '2022-03-15 10:30:00', 2),
('王五', 35, 1, '13800000003', '2021-06-20 11:00:00', 3);

-- 6. inspection 监测数据表
-- 假设 sensor 表的 id 从 1 开始自增
INSERT INTO inspection (pressure, temperature, traffic, shake, data_status, realtime_picture, sensor_id) VALUES
(10.5, 25.1, 100.2, 0.5, 0, NULL, 1),
(12.3, 26.0, 110.5, 0.8, 0, NULL, 2),
(15.0, 28.5, 120.0, 1.2, 2, 'http://example.com/pic1.jpg', 3), -- 危险数据
(9.8, 24.5, 95.0, 0.4, 0, NULL, 4);

-- 7. task 任务表
-- 假设 inspection 表的 id 从 1 开始自增, repairman 表的 id 从 1 开始自增
INSERT INTO task (inspection_id, repairman_id, result, public_time, response_time, accomplish_time, feedback_information) VALUES
(3, 3, 0, '2023-10-26 10:00:00', NULL, NULL, NULL), -- 针对 sensor 3 的危险数据，分配给王五
(1, 1, 2, '2023-10-25 09:00:00', '2023-10-25 09:30:00', '2023-10-25 11:00:00', '传感器1运行正常，无异常情况。');

-- 8. journal_inspection 监测数据日志表
-- 假设 inspection 表的 id 从 1 开始自增
INSERT INTO journal_inspection (inspection_id, pressure, temperature, traffic, shake, data_status) VALUES
(1, 10.4, 25.0, 100.0, 0.5, 0),
(1, 10.6, 25.2, 100.3, 0.5, 0),
(3, 14.9, 28.4, 119.8, 1.1, 2),
(3, 15.1, 28.6, 120.1, 1.3, 3); -- 高危数据

-- 9. journal_sensor 传感器日志表
-- 假设 sensor 表的 id 从 1 开始自增, repairman 表的 id 从 1 开始自增
INSERT INTO journal_sensor (sensor_id, status, overhaul_time, repairman_id) VALUES
(3, 1, '2023-10-26 10:30:00', 3), -- 传感器3异常，王五检修
(1, 0, NULL, NULL);