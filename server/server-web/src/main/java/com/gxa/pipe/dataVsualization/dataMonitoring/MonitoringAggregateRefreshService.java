package com.gxa.pipe.dataVsualization.dataMonitoring;

import com.gxa.pipe.config.MonitoringAggregateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringAggregateRefreshService {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final MonitoringAggregateProperties properties;

    @Scheduled(fixedDelayString = "${performance.monitoring-aggregate.today-refresh-interval-ms:600000}")
    public void refreshHotWindow() {
        if (!properties.isRefreshEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now();
        refreshDate(today.minusDays(1));
        refreshDate(today);
    }

    @Scheduled(cron = "${performance.monitoring-aggregate.repair-cron:0 20 3 * * *}")
    public void repairRecentWindow() {
        if (!properties.isRefreshEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now();
        int windowDays = Math.max(properties.getRepairWindowDays(), 1);
        for (int offset = 0; offset < windowDays; offset++) {
            refreshDate(today.minusDays(offset));
        }
    }

    public void refreshDate(LocalDate statDate) {
        try {
            transactionTemplate.executeWithoutResult(status -> refreshDateInTransaction(statDate));
        } catch (RuntimeException exception) {
            recordFailure(statDate, exception);
            log.warn("监测日聚合刷新失败，statDate={}", statDate, exception);
        }
    }

    private void refreshDateInTransaction(LocalDate statDate) {
        SourceFingerprint sourceFingerprint = readSourceFingerprint(statDate);
        RefreshState refreshState = readRefreshState(statDate);

        if (sourceFingerprint.sourceCount() == 0L) {
            deleteAggregateRows(statDate);
            deleteSummaryRow(statDate);
            upsertState(statDate, sourceFingerprint, 0, SUCCESS, null);
            return;
        }

        if (refreshState != null
                && SUCCESS.equals(refreshState.status())
                && refreshState.sourceCount() == sourceFingerprint.sourceCount()
                && sameTimestamp(refreshState.sourceMaxUpdateTime(), sourceFingerprint.sourceMaxUpdateTime())
                && refreshState.aggregateRows() == countAggregateRows(statDate)) {
            return;
        }

        deleteAggregateRows(statDate);
        int aggregateRows = insertAggregateRows(statDate);
        upsertSummaryRow(statDate);
        upsertState(statDate, sourceFingerprint, aggregateRows, SUCCESS, null);
    }

    private SourceFingerprint readSourceFingerprint(LocalDate statDate) {
        LocalDate nextDate = statDate.plusDays(1);
        return jdbcTemplate.queryForObject(
                """
                SELECT
                  COUNT(*) AS source_count,
                  MAX(update_time) AS source_max_update_time
                FROM inspection
                WHERE create_time >= ?
                  AND create_time < ?
                """,
                (rs, rowNum) -> new SourceFingerprint(
                        rs.getLong("source_count"),
                        rs.getTimestamp("source_max_update_time")),
                Timestamp.valueOf(statDate.atStartOfDay()),
                Timestamp.valueOf(nextDate.atStartOfDay()));
    }

    private RefreshState readRefreshState(LocalDate statDate) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT
                      source_count,
                      source_max_update_time,
                      aggregate_rows,
                      status
                    FROM inspection_metric_refresh_state
                    WHERE stat_date = ?
                    """,
                    (rs, rowNum) -> new RefreshState(
                            rs.getLong("source_count"),
                            rs.getTimestamp("source_max_update_time"),
                            rs.getLong("aggregate_rows"),
                            rs.getString("status")),
                    Date.valueOf(statDate));
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private long countAggregateRows(LocalDate statDate) {
        Long rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inspection_metric_daily WHERE stat_date = ?",
                Long.class,
                Date.valueOf(statDate));
        return rows == null ? 0L : rows;
    }

    private void deleteAggregateRows(LocalDate statDate) {
        jdbcTemplate.update(
                "DELETE FROM inspection_metric_daily WHERE stat_date = ?",
                Date.valueOf(statDate));
    }

    private void deleteSummaryRow(LocalDate statDate) {
        jdbcTemplate.update(
                "DELETE FROM inspection_metric_daily_summary WHERE stat_date = ?",
                Date.valueOf(statDate));
    }

    private int insertAggregateRows(LocalDate statDate) {
        LocalDate nextDate = statDate.plusDays(1);
        return jdbcTemplate.update(
                """
                INSERT INTO inspection_metric_daily (
                  stat_date,
                  area_id,
                  pipeline_id,
                  pipe_segment_id,
                  sample_count,
                  pressure_sum,
                  traffic_sum,
                  temperature_sum,
                  shake_sum,
                  safe_count,
                  good_count,
                  danger_count,
                  critical_count
                )
                SELECT
                  ? AS stat_date,
                  s.area_id,
                  s.pipeline_id,
                  s.pipe_segment_id,
                  COUNT(*) AS sample_count,
                  SUM(i.pressure) AS pressure_sum,
                  SUM(i.traffic) AS traffic_sum,
                  SUM(i.temperature) AS temperature_sum,
                  SUM(i.shake) AS shake_sum,
                  SUM(CASE WHEN i.data_status = 0 THEN 1 ELSE 0 END) AS safe_count,
                  SUM(CASE WHEN i.data_status = 1 THEN 1 ELSE 0 END) AS good_count,
                  SUM(CASE WHEN i.data_status = 2 THEN 1 ELSE 0 END) AS danger_count,
                  SUM(CASE WHEN i.data_status = 3 THEN 1 ELSE 0 END) AS critical_count
                FROM inspection i
                INNER JOIN sensor s ON i.sensor_id = s.id
                WHERE i.create_time >= ?
                  AND i.create_time < ?
                GROUP BY s.area_id, s.pipeline_id, s.pipe_segment_id
                """,
                Date.valueOf(statDate),
                Timestamp.valueOf(statDate.atStartOfDay()),
                Timestamp.valueOf(nextDate.atStartOfDay()));
    }

    private void upsertSummaryRow(LocalDate statDate) {
        jdbcTemplate.update(
                """
                INSERT INTO inspection_metric_daily_summary (
                  stat_date,
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
                  COALESCE(SUM(sample_count), 0) AS sample_count,
                  COALESCE(SUM(pressure_sum), 0) AS pressure_sum,
                  COALESCE(SUM(traffic_sum), 0) AS traffic_sum,
                  COALESCE(SUM(temperature_sum), 0) AS temperature_sum,
                  COALESCE(SUM(shake_sum), 0) AS shake_sum,
                  COALESCE(SUM(safe_count), 0) AS safe_count,
                  COALESCE(SUM(good_count), 0) AS good_count,
                  COALESCE(SUM(danger_count), 0) AS danger_count,
                  COALESCE(SUM(critical_count), 0) AS critical_count,
                  NOW() AS refreshed_at
                FROM inspection_metric_daily
                WHERE stat_date = ?
                GROUP BY stat_date
                ON DUPLICATE KEY UPDATE
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
                """,
                Date.valueOf(statDate));
    }

    private void upsertState(
            LocalDate statDate,
            SourceFingerprint sourceFingerprint,
            long aggregateRows,
            String status,
            String message) {
        jdbcTemplate.update(
                """
                INSERT INTO inspection_metric_refresh_state (
                  stat_date,
                  source_count,
                  source_max_update_time,
                  aggregate_rows,
                  refreshed_at,
                  status,
                  message
                ) VALUES (?, ?, ?, ?, NOW(), ?, ?)
                ON DUPLICATE KEY UPDATE
                  source_count = VALUES(source_count),
                  source_max_update_time = VALUES(source_max_update_time),
                  aggregate_rows = VALUES(aggregate_rows),
                  refreshed_at = VALUES(refreshed_at),
                  status = VALUES(status),
                  message = VALUES(message)
                """,
                Date.valueOf(statDate),
                sourceFingerprint.sourceCount(),
                sourceFingerprint.sourceMaxUpdateTime(),
                aggregateRows,
                status,
                message);
    }

    private void recordFailure(LocalDate statDate, RuntimeException exception) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO inspection_metric_refresh_state (
                      stat_date,
                      source_count,
                      source_max_update_time,
                      aggregate_rows,
                      refreshed_at,
                      status,
                      message
                    ) VALUES (?, 0, NULL, 0, NOW(), ?, ?)
                    ON DUPLICATE KEY UPDATE
                      refreshed_at = VALUES(refreshed_at),
                      status = VALUES(status),
                      message = VALUES(message)
                    """,
                    Date.valueOf(statDate),
                    FAILED,
                    trimMessage(exception));
        } catch (RuntimeException failureRecordException) {
            log.warn("监测日聚合失败状态写入失败，statDate={}", statDate, failureRecordException);
        }
    }

    private boolean sameTimestamp(Timestamp left, Timestamp right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.toInstant().equals(right.toInstant());
    }

    private String trimMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= 255 ? message : message.substring(0, 255);
    }

    private record SourceFingerprint(long sourceCount, Timestamp sourceMaxUpdateTime) {
    }

    private record RefreshState(
            long sourceCount,
            Timestamp sourceMaxUpdateTime,
            long aggregateRows,
            String status) {
    }
}
