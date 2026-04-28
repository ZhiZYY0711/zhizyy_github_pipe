package com.gxa.pipe.dataVsualization.taskDetail;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 任务详情业务逻辑实现类
 */
@Service
@RequiredArgsConstructor
public class TaskDetailsServiceImpl implements TaskDetailsService {
    
    private final TaskDetailsMapper taskDetailsMapper;
    
    /**
     * 获取任务状态分布
     * @param request 任务请求参数
     * @return 任务状态分布列表
     */
    @Override
    public List<TaskStatusResponse> getTaskStatusDistribution(TaskResquest request) {
        // 调用Mapper层查询数据
        return taskDetailsMapper.selectTaskStatusDistribution(
            request.getAreaId(), 
            normalizeUnixTimestamp(request.getStartTime()),
            normalizeUnixTimestamp(request.getEndTime())
        );
    }
    
    /**
     * 获取任务数量趋势
     * @param request 任务请求参数
     * @return 任务数量趋势列表
     */
    @Override
    public List<TaskNumberResponse> getTaskNumberTrend(TaskResquest request) {
        // 调用Mapper层查询数据
        return taskDetailsMapper.selectTaskNumberTrend(
            request.getAreaId(), 
            normalizeUnixTimestamp(request.getStartTime()),
            normalizeUnixTimestamp(request.getEndTime())
        );
    }
    
    /**
     * 获取区域任务对比
     * @param request 任务请求参数
     * @return 同级区域任务对比列表
     */
    @Override
    public List<AreaTaskResponse> getAreaTaskContrast(TaskResquest request) {
        int areaId = request.getAreaId();
        if (areaId == 0) {
            return taskDetailsMapper.selectAreaTaskContrast(
                100000,
                999999,
                "PROVINCE",
                normalizeUnixTimestamp(request.getStartTime()),
                normalizeUnixTimestamp(request.getEndTime())
            );
        }

        // 解析区域ID，获取下一级区域的范围
        String areaIdStr = String.format("%06d", areaId);
        
        // 根据区域ID的层级确定查询范围
        String provinceCode = areaIdStr.substring(0, 2);
        String cityCode = areaIdStr.substring(2, 4);
        String districtCode = areaIdStr.substring(4, 6);
        
        int startRange, endRange;
        String targetLevel;
        
        if ("00".equals(districtCode) && "00".equals(cityCode)) {
            // 省级：查询该省下的城市
            startRange = Integer.parseInt(provinceCode + "0000");
            endRange = Integer.parseInt(provinceCode + "9999");
            targetLevel = "CITY";
        } else if ("00".equals(districtCode)) {
            // 市级：查询该市下的区县
            startRange = Integer.parseInt(provinceCode + cityCode + "00");
            endRange = Integer.parseInt(provinceCode + cityCode + "99");
            targetLevel = "DISTRICT";
        } else {
            // 区县级：查询当前区县
            startRange = areaId;
            endRange = areaId;
            targetLevel = "SELF";
        }
        
        // 调用Mapper层查询下一级区域的任务数量
        return taskDetailsMapper.selectAreaTaskContrast(
            startRange,
            endRange,
            targetLevel,
            normalizeUnixTimestamp(request.getStartTime()),
            normalizeUnixTimestamp(request.getEndTime())
        );
    }
    
    /**
     * 获取KPI榜单
     * @param request KPI榜单请求参数
     * @return KPI榜单列表
     */
    @Override
    public List<TaskKpiResponse> getKpiList(TaskKpiRequest request) {
        // 根据榜单类型调用不同的查询方法
        switch (request.getType()) {
            case 0:
                // 完成任务数榜单
                return taskDetailsMapper.selectTaskCompletionRanking(
                    request.getAreaId(),
                    normalizeUnixTimestamp(request.getStartTime()),
                    normalizeUnixTimestamp(request.getEndTime())
                );
            case 1:
                // 平均响应时间榜单
                return taskDetailsMapper.selectAverageResponseTimeRanking(
                    request.getAreaId(),
                    normalizeUnixTimestamp(request.getStartTime()),
                    normalizeUnixTimestamp(request.getEndTime())
                );
            case 2:
                // 平均完成时间榜单
                return taskDetailsMapper.selectAverageCompletionTimeRanking(
                    request.getAreaId(),
                    normalizeUnixTimestamp(request.getStartTime()),
                    normalizeUnixTimestamp(request.getEndTime())
                );
            default:
                throw new IllegalArgumentException("不支持的榜单类型: " + request.getType());
        }
    }

    private long normalizeUnixTimestamp(long timestamp) {
        if (timestamp > 9_999_999_999L) {
            return timestamp / 1000;
        }
        return timestamp;
    }
}
