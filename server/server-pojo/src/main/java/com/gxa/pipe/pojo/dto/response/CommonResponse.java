package com.gxa.pipe.pojo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {

    /**
     * 响应代码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     */
    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(200, "success", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> CommonResponse<T> success() {
        return new CommonResponse<>(200, "success", null);
    }

    /**
     * 失败响应
     */
    public static <T> CommonResponse<T> error(String message) {
        return new CommonResponse<>(500, message, null);
    }

    /**
     * 自定义响应
     */
    public static <T> CommonResponse<T> custom(Integer code, String message, T data) {
        return new CommonResponse<>(code, message, data);
    }
}