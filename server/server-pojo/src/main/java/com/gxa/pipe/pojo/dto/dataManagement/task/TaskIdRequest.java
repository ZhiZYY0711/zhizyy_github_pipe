package com.gxa.pipe.pojo.dto.dataManagement.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 任务ID请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskIdRequest {

    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    private Long id;
}