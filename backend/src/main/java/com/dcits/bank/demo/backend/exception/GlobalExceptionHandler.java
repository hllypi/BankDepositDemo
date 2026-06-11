package com.dcits.bank.demo.backend.exception;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.enums.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getResultCode().getCode(), e.getMessage());
        return ApiResult.fail(e.getResultCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(ResultCode.SYSTEM_ERROR);
    }
}
