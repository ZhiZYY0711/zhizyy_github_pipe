# 总览大屏后端与数据库性能 Spec

## 决策

总览大屏后端性能优化采用“专用读模型 + 合并接口”的方案。大屏运行时不应频繁扫描 `inspection`、`sensor`、`task` 明细表来计算 KPI、告警和区域趋势，而应优先读取面向大屏预聚合的汇总表。

现有 `inspection_metric_daily`、`inspection_metric_daily_summary` 和 `MonitoringAggregateRefreshService` 可以作为基础，但还不够完整。需要新增大屏专用的区域日指标、当前 KPI 快照、近期告警快照，并提供一个合并后的 dashboard summary API，减少前端多接口并发和 hover 时的重复查询。

## 背景

总览页当前主要调用三类接口：`/data_visualization/all/whole_kpi` 获取传感器、告警和任务 KPI；`/data_visualization/all/running_water_alarm` 获取最近告警流水；区域 hover 时调用 `/data_visualization/data_monitoring/area_data` 和 `/data_visualization/all/whole_kpi` 组合 tooltip 数据。

这些接口背后的 SQL 仍有明显明细表压力。`TotalMapper.selectWholeKpi` 会在 `sensor`、`inspection`、`task` 上做多段 count 和 join。`TotalMapper.selectRunningWaterAlarm` 会 join `inspection`、`sensor`、`area`，按 `inspection.create_time` 倒序取最近 100 条。`DataMonitoringMapper.selectAreaDetails` 会从 `inspection` 按天聚合四维指标，区域筛选时 join `sensor`。

项目已经有性能优化基础：`database/mysql/02-performance-aggregate-indexes.sql` 创建了 `inspection_metric_daily`、`inspection_metric_refresh_state` 和 `inspection_metric_daily_summary`；`database/mysql/03-performance-summary-fast-paths.sql` 增加了 summary sum 字段和部分索引；`MonitoringAggregateRefreshService` 可以定时刷新今日、昨日和近几天聚合。但 `application.yml` 中 `PIPELINE_MONITORING_AGGREGATE_ENABLED` 和 `PIPELINE_MONITORING_AGGREGATE_REFRESH_ENABLED` 默认都是 `false`，并且 `whole_kpi`、`running_water_alarm` 仍没有完整走大屏读模型。

## 目标

第一个目标是让总览页常用接口在百万级 `inspection`、十万级 `sensor`、五万级 `task` 数据下稳定低延迟。大屏初始加载、区域切换、时间范围切换和 hover tooltip 都不应触发大范围明细聚合。

第二个目标是减少前端请求数量。总览页初始加载和区域切换应优先请求一个合并接口，而不是分别请求 KPI、告警、趋势和 tooltip 依赖数据。

第三个目标是把区域筛选从重复 SQL 片段升级为统一 scope 解析。省、市、区县和全国范围应在 Java 层解析成稳定的 `areaId`、`areaLevel`、`areaStart`、`areaEnd`，SQL 不再到处复制 `MOD(#{areaId})` 判断。

第四个目标是给数据新鲜度建立明确契约。不同数据允许不同刷新频率：告警要更接近实时，KPI 快照可以几十秒级，日趋势可以分钟级。

## 非目标

本 spec 不引入 PostGIS，不迁移 MySQL 到时序数据库，也不改动业务写入流程。

本 spec 不重做监测数据管理页面的明细分页查询。明细页可以继续按条件查 `inspection`，但 count、指标卡和总览页不能依赖明细扫描。

本 spec 不改变现有旧接口返回格式。旧接口可以保留兼容，但内部应尽量复用新的 dashboard 读服务。

## 架构方案

### 合并 API

新增 `DashboardSummaryController`，建议路径为 `/data_visualization/dashboard`。

`GET /data_visualization/dashboard/summary` 接收 `area_id`、`start_time`、`end_time`，返回当前区域的大屏首屏数据：KPI、最近告警、区域四维趋势、数据新鲜度和服务端生成时间。前端区域切换和时间范围切换优先走这个接口。

`GET /data_visualization/dashboard/area_tooltip` 接收 `area_id`、`start_time`、`end_time`，返回 hover tooltip 所需的区域名、传感器数、异常传感器数、告警数、平均压力、平均流量、平均温度和平均震动。这个接口替代 hover 时并发调用 `area_data` 和 `whole_kpi`。

旧接口 `/whole_kpi`、`/running_water_alarm`、`/area_data` 保留，但 service 内部优先调用新的 dashboard read service。这样可以降低前端迁移风险，也避免同一套指标逻辑分叉。

### 区域 Scope 解析

新增 `AreaScopeResolver`。输入可以为空、全国编码、省编码、市编码或区县编码，输出统一结构：

`areaId` 表示当前业务区域编码；`areaLevel` 表示 `COUNTRY`、`PROVINCE`、`CITY`、`DISTRICT`；`areaStart` 和 `areaEnd` 表示明细表兼容查询范围；`rollupAreaId` 表示汇总表精确查询 ID。

总览读模型优先使用 `rollupAreaId` 精确匹配。只有兼容旧明细查询或回退路径才使用 `areaStart`、`areaEnd` 范围查询。这样可以减少 SQL OR 条件，也更容易让 MySQL 使用明确索引。

### 大屏读模型表

保留 `inspection_metric_daily` 作为监测指标日事实表。它按 `stat_date + area_id + pipeline_id + pipe_segment_id` 聚合，适合从明细表刷新，也适合给其他汇总表提供输入。

新增 `dashboard_area_metric_daily`，作为大屏按行政层级读取的日指标汇总表。建议字段包括 `stat_date`、`area_id`、`area_level`、`sample_count`、`pressure_sum`、`traffic_sum`、`temperature_sum`、`shake_sum`、`safe_count`、`good_count`、`danger_count`、`critical_count`、`refreshed_at`。主键为 `stat_date + area_id`，核心索引为 `idx_dashboard_area_metric_area_date(area_id, stat_date)`。

新增 `dashboard_area_current_summary`，作为当前 KPI 快照表。建议字段包括 `area_id`、`area_level`、`sensor_count`、`abnormal_sensor_count`、`underway_task_count`、`overtime_task_count`、`refreshed_at`。主键为 `area_id`。这张表服务于 `whole_kpi` 中不适合用日监测指标表表达的当前状态类数据。

新增 `dashboard_alarm_recent`，作为近期告警快照表。建议字段包括 `inspection_id`、`alarm_time`、`sensor_id`、`area_id`、`area_name`、`message`、`level`、`data_status`、`pressure`、`traffic`、`temperature`、`shake`、`refreshed_at`。核心索引为 `idx_dashboard_alarm_area_time(area_id, alarm_time DESC)` 和 `idx_dashboard_alarm_time(alarm_time DESC)`。这张表用于避免大屏告警流水每次 join `inspection`、`sensor`、`area`。

新增或扩展刷新状态表，记录每类读模型的刷新时间、源数据指纹、行数、状态和错误消息。大屏 summary API 应返回 freshness 信息，便于前端和运维判断数据是否过期。

### 刷新策略

监测日指标分两层刷新。底层 `inspection_metric_daily` 继续由 `MonitoringAggregateRefreshService` 刷新今日、昨日和近几天窗口。上层 `dashboard_area_metric_daily` 从 `inspection_metric_daily` 汇总生成全国、省、市、区县四类区域行。这样查询时可以精确按 `area_id` 读取，不需要每次按省市范围扫很多 leaf 行。

当前 KPI 快照 `dashboard_area_current_summary` 建议每 30-60 秒刷新一次。刷新时按区域层级汇总 `sensor` 和 `task` 当前状态，写入全国、省、市、区县行。任务超时逻辑应在刷新 SQL 中使用可走索引的时间边界，例如 `public_time < NOW() - INTERVAL 24 HOUR` 和 `response_time < NOW() - INTERVAL 48 HOUR`，避免在列上包 `TIMESTAMPDIFF`。

近期告警快照 `dashboard_alarm_recent` 建议每 15-30 秒增量刷新，保留最近 24 小时或最近若干千条告警。插入时把 areaName、sensorName、level、message 等展示字段反规范化，避免读路径 join。若后续写入链路可控，可以把告警快照更新接到监测数据新增流程；第一阶段用定时增量刷新即可。

生产环境启用聚合读路径前，必须先执行全量 backfill，再打开 `PIPELINE_MONITORING_AGGREGATE_ENABLED=true` 和对应 dashboard read model 开关。刷新失败时接口可以短期回退旧查询，但必须记录 warn 日志和 freshness 状态，不能静默长期使用慢路径。

### 查询策略

`summary` 接口读取 `dashboard_area_current_summary` 得到当前传感器和任务 KPI，读取 `dashboard_area_metric_daily` 得到时间范围内的 warning 数和四维趋势，读取 `dashboard_alarm_recent` 得到告警流水。

`area_tooltip` 接口只读取 `dashboard_area_current_summary` 和 `dashboard_area_metric_daily`，不读取 `inspection` 明细。时间范围跨多天时按 `stat_date` 聚合；无时间范围时按当前前端 `all` 语义处理，即读取该区域所有已聚合日期并返回全量平均值。

如果某个区域没有汇总行，接口应返回零值和 freshness 信息，而不是回退到大范围明细扫描。开发环境可以通过配置允许回退，生产环境默认不回退，以避免偶发慢查询拖垮大屏。

### 缓存策略

在 `server-web` 内新增轻量本地 TTL 缓存，不引入外部 Redis 依赖。缓存 key 由接口名、`area_id`、`start_time`、`end_time` 和数据版本组成。

`summary` 缓存 TTL 建议 10-30 秒；`area_tooltip` 缓存 TTL 建议 30-60 秒；区域下拉数据缓存 TTL 可以更长。缓存命中时仍返回后端 `generatedAt` 和读模型 `freshness`，方便定位数据过期问题。

如果未来部署多实例且需要严格一致，可以再切到 Redis 或共享缓存。但第一阶段不应该因为缓存基础设施阻塞数据库读模型优化。

## 数据库索引要求

现有 `inspection_metric_daily` 需要保留 `idx_inspection_metric_daily_area_date(area_id, stat_date)`，并新增 `idx_inspection_metric_daily_date_area(stat_date, area_id)`，除非目标环境已经存在等价索引。此前排查中观察到 MySQL 优化器可能选错索引，聚合查询应通过更明确的 SQL 条件或必要时 `FORCE INDEX` 避免退化。

`dashboard_area_metric_daily` 必须有 `PRIMARY KEY(stat_date, area_id)` 和 `idx_dashboard_area_metric_area_date(area_id, stat_date)`。

`dashboard_area_current_summary` 必须以 `area_id` 为主键。

`dashboard_alarm_recent` 必须有 `idx_dashboard_alarm_area_time(area_id, alarm_time DESC)` 和 `idx_dashboard_alarm_time(alarm_time DESC)`。

原始表仍需要服务刷新任务和明细页。`inspection` 至少保留 `idx_inspection_status_create_time(data_status, create_time)`、`idx_inspection_sensor_time(sensor_id, create_time)`，并新增 `idx_inspection_status_time_sensor(data_status, create_time, sensor_id)`，除非目标环境已经存在等价索引。`sensor` 至少保留 `idx_sensor_area_pipe_segment(area_id, pipeline_id, pipe_segment_id)`。`task` 需要覆盖 `status + area_id`、`status + public_time`、`status + response_time`、`status + accomplish_time` 等大屏 KPI 路径。

## 验收标准

新增 MySQL 迁移脚本必须可以重复执行，创建大屏读模型表、索引和必要的 backfill SQL。迁移脚本不能破坏已有 `inspection_metric_daily`、`inspection_metric_daily_summary` 和旧接口。

聚合刷新开启后，刷新状态表必须能看到 `dashboard_area_metric_daily`、`dashboard_area_current_summary` 和 `dashboard_alarm_recent` 的成功刷新记录、行数和刷新时间。

`summary` 和 `area_tooltip` 接口必须返回现有前端所需全部字段，并且旧接口在兼容期内仍可使用。

在当前开发数据规模下，`summary`、`area_tooltip`、`whole_kpi`、`running_water_alarm` 的读路径不应对 `inspection` 做大范围明细聚合。通过 SQL 日志或 `EXPLAIN` 验证应主要命中大屏汇总表或告警快照表。

性能目标是在同一开发环境中，区域切换时后端 summary API p95 小于 120 ms，hover tooltip API p95 小于 80 ms，告警流水读取 p95 小于 120 ms。具体数值会受机器影响，但必须相对当前 raw 查询路径有明显下降。

## 风险和取舍

读模型会引入数据新鲜度延迟。这个取舍是可接受的，因为大屏需要稳定响应和整体态势，不应该每次交互都临时扫描明细表。告警快照刷新频率应比日趋势更高，以减少感知延迟。

多张汇总表会增加刷新逻辑复杂度。为降低风险，刷新任务必须记录状态和错误，接口必须暴露 freshness，并保留短期旧接口兼容。

当前项目没有数据库迁移框架，MySQL 迁移仍需手动执行。spec 要求迁移脚本幂等，是为了降低手动执行风险，但不能替代上线流程中的备份和灰度。

本地 TTL 缓存在多实例部署时不是强一致。对大屏只读指标来说可以接受；如后续要强一致或跨实例缓存，再引入 Redis。

## 验证计划

先写 mapper/service 单元测试，覆盖全国、省、市、区县四类 `AreaScope` 解析，验证 summary 和 tooltip 使用汇总表返回正确零值、聚合值和 freshness。

再执行 MySQL 迁移和 backfill，在开发库检查新表行数、索引和刷新状态。关键查询需要跑 `EXPLAIN`，确认没有回到 `inspection` 全表扫描。

然后运行 `mvn -pl server-web -am test` 或至少运行新增的 service/mapper 相关测试，再运行 `mvn -pl server-web -am compile -DskipTests`。

最后配合前端 Playwright 采样 `/dashboard`。区域切换、时间范围切换和 hover 时记录网络请求数量、接口耗时和服务端日志，确认前端优先使用合并 API，旧接口没有造成额外明细扫描。
