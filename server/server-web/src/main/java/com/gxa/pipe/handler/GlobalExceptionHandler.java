package com.gxa.pipe.handler;

import com.gxa.pipe.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常 - @RequestBody参数校验
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常：{}", e.getMessage());
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder errorMessage = new StringBuilder("参数校验失败：");
        
        for (FieldError fieldError : fieldErrors) {
            errorMessage.append(fieldError.getField())
                       .append(" ")
                       .append(fieldError.getDefaultMessage())
                       .append("; ");
        }
        
        return Result.error(errorMessage.toString());
    }

    /**
     * 处理参数校验异常 - @ModelAttribute参数校验
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleBindException(BindException e) {
        log.error("参数绑定异常：{}", e.getMessage());
        
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        StringBuilder errorMessage = new StringBuilder("参数校验失败：");
        
        for (FieldError fieldError : fieldErrors) {
            errorMessage.append(fieldError.getField())
                       .append(" ")
                       .append(fieldError.getDefaultMessage())
                       .append("; ");
        }
        
        return Result.error(errorMessage.toString());
    }

    /**
     * 处理参数校验异常 - @RequestParam和@PathVariable参数校验
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("约束违反异常：{}", e.getMessage());
        
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        StringBuilder errorMessage = new StringBuilder("参数校验失败：");
        
        for (ConstraintViolation<?> violation : violations) {
            errorMessage.append(violation.getPropertyPath())
                       .append(" ")
                       .append(violation.getMessage())
                       .append("; ");
        }
        
        return Result.error(errorMessage.toString());
    }

    /**
     * 处理数据库访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleDataAccessException(DataAccessException e) {
        log.error("数据库访问异常：{}", e.getMessage(), e);
        return Result.error("数据库操作失败，请稍后重试");
    }

    /**
     * 处理SQL异常
     */
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleSQLException(SQLException e) {
        log.error("SQL异常：{}", e.getMessage(), e);
        return Result.error("数据库操作异常，请联系管理员");
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleNullPointerException(NullPointerException e) {
        log.error("空指针异常：{}", e.getMessage(), e);
        return Result.error("系统内部错误，请联系管理员");
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数异常：{}", e.getMessage());
        return Result.error("参数错误：" + e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常：{}", e.getMessage(), e);
        return Result.error("系统运行异常，请稍后重试");
    }

    /**
     * 处理其他所有异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(Exception e) {
        log.error("未知异常：{}", e.getMessage(), e);
        return Result.error("系统异常，请联系管理员");
    }
}