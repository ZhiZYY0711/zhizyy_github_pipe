package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.config.DashboardReadModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardReadModelRefreshService {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final DashboardReadModelProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void refreshOnStartup() {
        if (properties.isRefreshEnabled() && properties.isRefreshOnStartup()) {
            refreshAll();
        }
    }

    @Scheduled(
            fixedDelayString = "${performance.dashboard-read-model.refresh-interval-ms:600000}",
            initialDelayString = "${performance.dashboard-read-model.initial-delay-ms:30000}")
    public void refreshScheduled() {
        if (!properties.isRefreshEnabled()) {
            return;
        }

        refreshAll();
    }

    public void refreshAll() {
        refreshDashboardAreaMetricDaily();
        refreshDashboardAreaCurrentSummary();
        refreshDashboardAlarmRecent();
        refreshLogIndicatorSummary();
        refreshLogDailySummary();
    }

    private void refreshDashboardAreaMetricDaily() {
        refreshModel("dashboard_area_metric_daily", () -> {
            jdbcTemplate.update("DELETE FROM dashboard_area_metric_daily");
            jdbcTemplate.update(INSERT_DASHBOARD_AREA_METRIC_DAILY_SQL);
            recordSuccess(
                    "dashboard_area_metric_daily",
                    "SELECT COUNT(*) FROM inspection_metric_daily",
                    "SELECT COUNT(*) FROM dashboard_area_metric_daily");
        });
    }

    private void refreshDashboardAreaCurrentSummary() {
        refreshModel("dashboard_area_current_summary", () -> {
            jdbcTemplate.update("DELETE FROM dashboard_area_current_summary");
            jdbcTemplate.update(INSERT_DASHBOARD_AREA_CURRENT_SUMMARY_SQL);
            recordSuccess(
                    "dashboard_area_current_summary",
                    "SELECT COUNT(*) FROM sensor",
                    "SELECT COUNT(*) FROM dashboard_area_current_summary");
        });
    }

    private void refreshDashboardAlarmRecent() {
        refreshModel("dashboard_alarm_recent", () -> {
            jdbcTemplate.update("DELETE FROM dashboard_alarm_recent");
            jdbcTemplate.update(INSERT_DASHBOARD_ALARM_RECENT_SQL, Math.max(properties.getAlarmLimit(), 1));
            recordSuccess(
                    "dashboard_alarm_recent",
                    "SELECT COUNT(*) FROM inspection WHERE data_status IN (2, 3)",
                    "SELECT COUNT(*) FROM dashboard_alarm_recent");
        });
    }

    private void refreshLogIndicatorSummary() {
        refreshModel("log_indicator_summary", () -> {
            jdbcTemplate.update(INSERT_LOG_INDICATOR_SUMMARY_SQL);
            recordSuccess(
                    "log_indicator_summary",
                    "SELECT COUNT(*) FROM `log`",
                    "SELECT COUNT(*) FROM log_indicator_summary");
        });
    }

    private void refreshLogDailySummary() {
        refreshModel("log_daily_summary", () -> {
            jdbcTemplate.update("DELETE FROM log_daily_summary");
            jdbcTemplate.update(INSERT_LOG_DAILY_SUMMARY_SQL);
            recordSuccess(
                    "log_daily_summary",
                    "SELECT COUNT(*) FROM `log` WHERE operation_time IS NOT NULL",
                    "SELECT COUNT(*) FROM log_daily_summary");
        });
    }

    private void refreshModel(String modelName, Runnable refreshAction) {
        try {
            transactionTemplate.executeWithoutResult(status -> refreshAction.run());
        } catch (RuntimeException exception) {
            recordFailure(modelName, exception);
            log.warn("读模型刷新失败，modelName={}", modelName, exception);
        }
    }

    private void recordSuccess(String modelName, String sourceCountSql, String readModelRowsSql) {
        jdbcTemplate.update("""
                REPLACE INTO dashboard_read_model_refresh_state (
                  model_name,
                  source_count,
                  read_model_rows,
                  refreshed_at,
                  status,
                  message
                )
                SELECT
                  '%s',
                  COALESCE((%s), 0),
                  COALESCE((%s), 0),
                  NOW(),
                  '%s',
                  NULL
                """.formatted(modelName, sourceCountSql, readModelRowsSql, SUCCESS));
    }

    private void recordFailure(String modelName, RuntimeException exception) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO dashboard_read_model_refresh_state (
                      model_name,
                      source_count,
                      read_model_rows,
                      refreshed_at,
                      status,
                      message
                    ) VALUES (?, 0, 0, NOW(), ?, ?)
                    ON DUPLICATE KEY UPDATE
                      refreshed_at = VALUES(refreshed_at),
                      status = VALUES(status),
                      message = VALUES(message)
                    """,
                    modelName,
                    FAILED,
                    trimMessage(exception));
        } catch (RuntimeException failureRecordException) {
            log.warn("读模型刷新失败状态写入失败，modelName={}", modelName, failureRecordException);
        }
    }

    private String trimMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= 255 ? message : message.substring(0, 255);
    }

    private static final String INSERT_DASHBOARD_AREA_METRIC_DAILY_SQL = """
            INSERT INTO dashboard_area_metric_daily (
              stat_date,
              area_id,
              area_level,
              sample_count,
              pressure_sum,
              traffic_sum,
              temperature_sum,
              shake_sum,
              safe_count,
              good_count,
              danger_count,
              critical_count,
              refreshed_at
            )
            SELECT
              stat_date,
              area_id,
              CASE
                WHEN area_id = 100000 THEN 'COUNTRY'
                WHEN MOD(area_id, 10000) = 0 THEN 'PROVINCE'
                WHEN MOD(area_id, 100) = 0 THEN 'CITY'
                ELSE 'DISTRICT'
              END AS area_level,
              SUM(sample_count) AS sample_count,
              SUM(pressure_sum) AS pressure_sum,
              SUM(traffic_sum) AS traffic_sum,
              SUM(temperature_sum) AS temperature_sum,
              SUM(shake_sum) AS shake_sum,
              SUM(safe_count) AS safe_count,
              SUM(good_count) AS good_count,
              SUM(danger_count) AS danger_count,
              SUM(critical_count) AS critical_count,
              NOW() AS refreshed_at
            FROM (
              SELECT
                stat_date,
                area_id,
                sample_count,
                pressure_sum,
                traffic_sum,
                temperature_sum,
                shake_sum,
                safe_count,
                good_count,
                danger_count,
                critical_count
              FROM inspection_metric_daily
              UNION ALL
              SELECT
                stat_date,
                CAST(FLOOR(area_id / 100) * 100 AS UNSIGNED) AS area_id,
                sample_count,
                pressure_sum,
                traffic_sum,
                temperature_sum,
                shake_sum,
                safe_count,
                good_count,
                danger_count,
                critical_count
              FROM inspection_metric_daily
              WHERE MOD(area_id, 100) != 0
              UNION ALL
              SELECT
                stat_date,
                CAST(FLOOR(area_id / 10000) * 10000 AS UNSIGNED) AS area_id,
                sample_count,
                pressure_sum,
                traffic_sum,
                temperature_sum,
                shake_sum,
                safe_count,
                good_count,
                danger_count,
                critical_count
              FROM inspection_metric_daily
              WHERE MOD(area_id, 10000) != 0
              UNION ALL
              SELECT
                stat_date,
                100000 AS area_id,
                sample_count,
                pressure_sum,
                traffic_sum,
                temperature_sum,
                shake_sum,
                safe_count,
                good_count,
                danger_count,
                critical_count
              FROM inspection_metric_daily
            ) metric_rows
            GROUP BY stat_date, area_id
            ON DUPLICATE KEY UPDATE
              area_level = VALUES(area_level),
              sample_count = VALUES(sample_count),
              pressure_sum = VALUES(pressure_sum),
              traffic_sum = VALUES(traffic_sum),
              temperature_sum = VALUES(temperature_sum),
              shake_sum = VALUES(shake_sum),
              safe_count = VALUES(safe_count),
              good_count = VALUES(good_count),
              danger_count = VALUES(danger_count),
              critical_count = VALUES(critical_count),
              refreshed_at = VALUES(refreshed_at)
            """;

    private static final String INSERT_DASHBOARD_AREA_CURRENT_SUMMARY_SQL = """
            INSERT INTO dashboard_area_current_summary (
              area_id,
              area_level,
              sensor_count,
              abnormal_sensor_count,
              underway_task_count,
              overtime_task_count,
              refreshed_at
            )
            WITH
            sensor_scope_rows AS (
              SELECT
                area_id,
                1 AS sensor_count,
                CASE WHEN status IN (1, 2) THEN 1 ELSE 0 END AS abnormal_sensor_count
              FROM sensor
              WHERE area_id IS NOT NULL
              UNION ALL
              SELECT
                CAST(FLOOR(area_id / 100) * 100 AS UNSIGNED) AS area_id,
                1 AS sensor_count,
                CASE WHEN status IN (1, 2) THEN 1 ELSE 0 END AS abnormal_sensor_count
              FROM sensor
              WHERE area_id IS NOT NULL
                AND MOD(area_id, 100) != 0
              UNION ALL
              SELECT
                CAST(FLOOR(area_id / 10000) * 10000 AS UNSIGNED) AS area_id,
                1 AS sensor_count,
                CASE WHEN status IN (1, 2) THEN 1 ELSE 0 END AS abnormal_sensor_count
              FROM sensor
              WHERE area_id IS NOT NULL
                AND MOD(area_id, 10000) != 0
              UNION ALL
              SELECT
                100000 AS area_id,
                1 AS sensor_count,
                CASE WHEN status IN (1, 2) THEN 1 ELSE 0 END AS abnormal_sensor_count
              FROM sensor
              WHERE area_id IS NOT NULL
            ),
            sensor_rows AS (
              SELECT
                area_id,
                COUNT(*) AS sensor_count,
                SUM(abnormal_sensor_count) AS abnormal_sensor_count
              FROM sensor_scope_rows
              GROUP BY area_id
            ),
            task_scope_rows AS (
              SELECT
                area_id,
                CASE WHEN status IN (0, 1) THEN 1 ELSE 0 END AS underway_task_count,
                CASE
                  WHEN status = 0 AND public_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1
                  WHEN status = 1 AND response_time < DATE_SUB(NOW(), INTERVAL 48 HOUR) THEN 1
                  WHEN status = 3 THEN 1
                  ELSE 0
                END AS overtime_task_count
              FROM task
              WHERE area_id IS NOT NULL
              UNION ALL
              SELECT
                CAST(FLOOR(area_id / 100) * 100 AS UNSIGNED) AS area_id,
                CASE WHEN status IN (0, 1) THEN 1 ELSE 0 END AS underway_task_count,
                CASE
                  WHEN status = 0 AND public_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1
                  WHEN status = 1 AND response_time < DATE_SUB(NOW(), INTERVAL 48 HOUR) THEN 1
                  WHEN status = 3 THEN 1
                  ELSE 0
                END AS overtime_task_count
              FROM task
              WHERE area_id IS NOT NULL
                AND MOD(area_id, 100) != 0
              UNION ALL
              SELECT
                CAST(FLOOR(area_id / 10000) * 10000 AS UNSIGNED) AS area_id,
                CASE WHEN status IN (0, 1) THEN 1 ELSE 0 END AS underway_task_count,
                CASE
                  WHEN status = 0 AND public_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1
                  WHEN status = 1 AND response_time < DATE_SUB(NOW(), INTERVAL 48 HOUR) THEN 1
                  WHEN status = 3 THEN 1
                  ELSE 0
                END AS overtime_task_count
              FROM task
              WHERE area_id IS NOT NULL
                AND MOD(area_id, 10000) != 0
              UNION ALL
              SELECT
                100000 AS area_id,
                CASE WHEN status IN (0, 1) THEN 1 ELSE 0 END AS underway_task_count,
                CASE
                  WHEN status = 0 AND public_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1
                  WHEN status = 1 AND response_time < DATE_SUB(NOW(), INTERVAL 48 HOUR) THEN 1
                  WHEN status = 3 THEN 1
                  ELSE 0
                END AS overtime_task_count
              FROM task
              WHERE area_id IS NOT NULL
            ),
            task_rows AS (
              SELECT
                area_id,
                SUM(underway_task_count) AS underway_task_count,
                SUM(overtime_task_count) AS overtime_task_count
              FROM task_scope_rows
              GROUP BY area_id
            ),
            area_rows AS (
              SELECT DISTINCT area_id FROM dashboard_area_metric_daily
              UNION
              SELECT DISTINCT area_id FROM sensor_rows
              UNION
              SELECT DISTINCT area_id FROM task_rows
            )
            SELECT
              area_rows.area_id,
              CASE
                WHEN area_rows.area_id = 100000 THEN 'COUNTRY'
                WHEN MOD(area_rows.area_id, 10000) = 0 THEN 'PROVINCE'
                WHEN MOD(area_rows.area_id, 100) = 0 THEN 'CITY'
                ELSE 'DISTRICT'
              END AS area_level,
              COALESCE(sensor_rows.sensor_count, 0) AS sensor_count,
              COALESCE(sensor_rows.abnormal_sensor_count, 0) AS abnormal_sensor_count,
              COALESCE(task_rows.underway_task_count, 0) AS underway_task_count,
              COALESCE(task_rows.overtime_task_count, 0) AS overtime_task_count,
              NOW() AS refreshed_at
            FROM area_rows
            LEFT JOIN sensor_rows ON sensor_rows.area_id = area_rows.area_id
            LEFT JOIN task_rows ON task_rows.area_id = area_rows.area_id
            ON DUPLICATE KEY UPDATE
              area_level = VALUES(area_level),
              sensor_count = VALUES(sensor_count),
              abnormal_sensor_count = VALUES(abnormal_sensor_count),
              underway_task_count = VALUES(underway_task_count),
              overtime_task_count = VALUES(overtime_task_count),
              refreshed_at = VALUES(refreshed_at)
            """;

    private static final String INSERT_DASHBOARD_ALARM_RECENT_SQL = """
            INSERT INTO dashboard_alarm_recent (
              inspection_id,
              alarm_time,
              sensor_id,
              sensor_name,
              area_id,
              area_name,
              message,
              level,
              data_status,
              pressure,
              traffic,
              temperature,
              shake,
              refreshed_at
            )
            SELECT
              i.`id`,
              i.`create_time`,
              s.`id`,
              CONCAT('传感器-', s.`id`),
              s.`area_id`,
              CONCAT(a.`province`, a.`city`, a.`district`),
              CASE
                WHEN i.`data_status` = 2 THEN '危险状态检测到异常数据'
                WHEN i.`data_status` = 3 THEN '高危状态检测到严重异常数据'
                ELSE '检测到异常数据'
              END,
              CASE
                WHEN i.`data_status` = 2 THEN '危险'
                WHEN i.`data_status` = 3 THEN '高危'
                ELSE '异常'
              END,
              i.`data_status`,
              i.`pressure`,
              i.`traffic`,
              i.`temperature`,
              i.`shake`,
              NOW()
            FROM inspection i
            INNER JOIN sensor s ON i.`sensor_id` = s.`id`
            INNER JOIN area a ON s.`area_id` = a.`id`
            WHERE i.`data_status` IN (2, 3)
            ORDER BY i.`create_time` DESC
            LIMIT ?
            ON DUPLICATE KEY UPDATE
              alarm_time = VALUES(alarm_time),
              sensor_id = VALUES(sensor_id),
              sensor_name = VALUES(sensor_name),
              area_id = VALUES(area_id),
              area_name = VALUES(area_name),
              message = VALUES(message),
              level = VALUES(level),
              data_status = VALUES(data_status),
              pressure = VALUES(pressure),
              traffic = VALUES(traffic),
              temperature = VALUES(temperature),
              shake = VALUES(shake),
              refreshed_at = VALUES(refreshed_at)
            """;

    private static final String INSERT_LOG_INDICATOR_SUMMARY_SQL = """
            INSERT INTO log_indicator_summary (
              id,
              total_count,
              success_count,
              error_count,
              warning_count,
              debugging_count,
              period_sum,
              refreshed_at
            )
            SELECT
              1 AS id,
              COUNT(*) AS total_count,
              COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS success_count,
              COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS error_count,
              COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS warning_count,
              COALESCE(SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END), 0) AS debugging_count,
              COALESCE(SUM(COALESCE(period, 0)), 0) AS period_sum,
              NOW() AS refreshed_at
            FROM `log`
            ON DUPLICATE KEY UPDATE
              total_count = VALUES(total_count),
              success_count = VALUES(success_count),
              error_count = VALUES(error_count),
              warning_count = VALUES(warning_count),
              debugging_count = VALUES(debugging_count),
              period_sum = VALUES(period_sum),
              refreshed_at = VALUES(refreshed_at)
            """;

    private static final String INSERT_LOG_DAILY_SUMMARY_SQL = """
            INSERT INTO log_daily_summary (
              stat_date,
              total_count,
              refreshed_at
            )
            SELECT
              DATE(operation_time) AS stat_date,
              COUNT(*) AS total_count,
              NOW() AS refreshed_at
            FROM `log`
            WHERE operation_time IS NOT NULL
            GROUP BY DATE(operation_time)
            ON DUPLICATE KEY UPDATE
              total_count = VALUES(total_count),
              refreshed_at = VALUES(refreshed_at)
            """;
}
