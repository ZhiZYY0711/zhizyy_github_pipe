package com.gxa.pipe.dataVsualization.dashboard;

import com.gxa.pipe.config.DashboardReadModelProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DashboardReadModelRefreshServiceTest {

    @Test
    void scheduledRefreshDoesNothingWhenRefreshIsDisabled() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        DashboardReadModelProperties properties = new DashboardReadModelProperties();
        properties.setRefreshEnabled(false);
        DashboardReadModelRefreshService service =
                new DashboardReadModelRefreshService(jdbcTemplate, transactionTemplate, properties);

        service.refreshScheduled();

        verifyNoInteractions(jdbcTemplate, transactionTemplate);
    }

    @Test
    void refreshAllRebuildsDashboardAndLogReadModels() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        DashboardReadModelProperties properties = new DashboardReadModelProperties();
        properties.setAlarmLimit(5000);
        DashboardReadModelRefreshService service =
                new DashboardReadModelRefreshService(jdbcTemplate, transactionTemplate, properties);
        runTransactionsImmediately(transactionTemplate);

        service.refreshAll();

        verify(jdbcTemplate).update(eq("DELETE FROM dashboard_area_metric_daily"));
        verify(jdbcTemplate).update(eq("DELETE FROM dashboard_area_current_summary"));
        verify(jdbcTemplate).update(eq("DELETE FROM dashboard_alarm_recent"));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("INSERT INTO dashboard_alarm_recent")
                && sql.contains("ORDER BY i.`create_time` DESC")
                && !sql.contains("DATE_SUB(NOW(), INTERVAL 1 DAY)")), eq(5000));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("INSERT INTO log_indicator_summary")));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("INSERT INTO log_daily_summary")));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("'dashboard_area_metric_daily'")));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("'dashboard_area_current_summary'")));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("'dashboard_alarm_recent'")));
    }

    private void runTransactionsImmediately(TransactionTemplate transactionTemplate) {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }
}
