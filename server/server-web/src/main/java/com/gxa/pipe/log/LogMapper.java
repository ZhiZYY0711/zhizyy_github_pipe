package com.gxa.pipe.log;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LogMapper {

    List<LogQueryResponse> selectLogsByParams(
            @Param("request") LogQueryRequest request,
            @Param("offset") int offset,
            @Param("limit") int limit);

    Long countLogsByParams(LogQueryRequest request);

    String selectUsernameByUserIdAndType(@Param("userId") Long userId, @Param("type") Integer type);

    LogQueryResponse selectLogById(@Param("id") Long id);

    LogIndicardResponse selectLogIndicators(@Param("areaId") Long areaId);
}
