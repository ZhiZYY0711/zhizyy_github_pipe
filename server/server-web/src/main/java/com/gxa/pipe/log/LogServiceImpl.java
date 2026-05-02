package com.gxa.pipe.log;

import com.gxa.pipe.utils.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;

    @Override
    public PageResult<LogQueryResponse> queryLogs(LogQueryRequest request) {
        log.info("Query logs by page: {}", request);

        int pageNum = request.getPage() != null && request.getPage() > 0 ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null && request.getPageSize() > 0 ? request.getPageSize() : 50;
        int offset = (pageNum - 1) * pageSize;

        Long total = logMapper.countLogsByParams(request);
        List<LogQueryResponse> records =
                total != null && total > 0
                        ? logMapper.selectLogsByParams(request, offset, pageSize)
                        : Collections.emptyList();

        PageResult<LogQueryResponse> result = new PageResult<>();
        result.setTotal(total != null ? total : 0L);
        result.setRecords(records);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);

        log.info("Query logs completed: total={}, currentPageSize={}", result.getTotal(), records.size());
        return result;
    }

    @Override
    public LogQueryResponse getLogById(Long id) {
        log.info("Query log by id={}", id);

        if (id == null || id <= 0) {
            log.warn("Invalid log id={}", id);
            throw new IllegalArgumentException("Log id must be greater than 0");
        }

        LogQueryResponse logResponse = logMapper.selectLogById(id);
        if (logResponse == null) {
            log.warn("Log record not found for id={}", id);
            throw new RuntimeException("Log record not found");
        }

        log.info("Query log by id completed: id={}, operate={}", id, logResponse.getOperate());
        return logResponse;
    }

    @Override
    public LogIndicardResponse getLogIndicatorCard(Long areaId) {
        log.info("Query log indicator card, areaId={}", areaId);

        LogIndicardResponse response = selectLogIndicators(areaId);
        if (response == null) {
            response = new LogIndicardResponse();
        }

        log.info(
                "Query log indicator card completed: areaId={}, all={}, warning={}, error={}, success={}, debugging={}, today={}, avgDuration={}",
                areaId,
                response.getAll(),
                response.getWarning(),
                response.getError(),
                response.getSuccess(),
                response.getDebugging(),
                response.getToday(),
                response.getAvgDuration());

        return response;
    }

    private LogIndicardResponse selectLogIndicators(Long areaId) {
        if (areaId == null || areaId <= 0) {
            try {
                LogIndicardResponse summary = logMapper.selectLogIndicatorsFromSummary();
                if (summary != null) {
                    return summary;
                }
            } catch (RuntimeException exception) {
                log.warn("Query log indicator summary failed, fallback to raw aggregate", exception);
            }
        }
        return logMapper.selectLogIndicators(areaId);
    }
}
