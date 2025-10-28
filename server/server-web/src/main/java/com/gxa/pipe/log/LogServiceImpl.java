package com.gxa.pipe.log;

import com.gxa.pipe.utils.PageResult;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogServiceImpl implements LogService {

    private final LogMapper logMapper;

    @Override
    public PageResult<LogQueryResponse> queryLogs(LogQueryRequest request) {
        log.info("分页查询日志，请求参数：{}", request);

        // 设置分页参数
        PageHelper.startPage(request.getPage(), request.getPageSize());

        // 查询日志数据（SQL查询中已通过JOIN获取用户名）
        Page<LogQueryResponse> page = (Page<LogQueryResponse>) logMapper.selectLogsByParams(request);

        // 构建分页结果
        PageResult<LogQueryResponse> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setRecords(page.getResult());
        result.setPageNum(request.getPage());
        result.setPageSize(request.getPageSize());

        log.info("查询日志完成，总记录数：{}，当前页记录数：{}", page.getTotal(), page.getResult().size());

        return result;
    }

    @Override
    public LogQueryResponse getLogById(Long id) {
        log.info("根据ID查询日志详细信息，日志ID：{}", id);

        if (id == null || id <= 0) {
            log.warn("日志ID无效：{}", id);
            throw new IllegalArgumentException("日志ID不能为空或小于等于0");
        }

        LogQueryResponse logResponse = logMapper.selectLogById(id);

        if (logResponse == null) {
            log.warn("未找到ID为{}的日志记录", id);
            throw new RuntimeException("未找到指定的日志记录");
        }

        log.info("查询日志详细信息完成，日志ID：{}，操作：{}", id, logResponse.getOperate());

        return logResponse;
    }

    @Override
    public LogIndicardResponse getLogIndicatorCard(Long areaId) {
        log.info("获取日志指标卡，区域ID：{}", areaId);

        try {
            // 统计全部日志数量
            Long allLogs = logMapper.countAllLogs(areaId);

            // 统计警告日志数量（状态为2）
            Long warningLogs = logMapper.countWarningLogs(areaId);

            // 统计错误日志数量（状态为1）
            Long errorLogs = logMapper.countErrorLogs(areaId);

            // 统计调试日志数量（状态为3）
            Long debuggingLogs = logMapper.countDebuggingLogs(areaId);

            // 统计今日日志数量
            Long todayLogs = logMapper.countTodayLogs(areaId);

            // 构建响应对象
            LogIndicardResponse response = new LogIndicardResponse();
            response.setAll(allLogs != null ? allLogs : 0L);
            response.setWarning(warningLogs != null ? warningLogs : 0L);
            response.setError(errorLogs != null ? errorLogs : 0L);
            response.setDebugging(debuggingLogs != null ? debuggingLogs : 0L);
            response.setToday(todayLogs != null ? todayLogs : 0L);

            log.info("获取日志指标卡完成，区域ID：{}，全部：{}，警告：{}，错误：{}，调试：{}，今日：{}",
                    areaId, response.getAll(), response.getWarning(), response.getError(),
                    response.getDebugging(), response.getToday());

            return response;
        } catch (Exception e) {
            log.error("获取日志指标卡失败，区域ID：{}，错误：{}", areaId, e.getMessage(), e);
            throw new RuntimeException("获取日志指标卡失败：" + e.getMessage());
        }
    }
}