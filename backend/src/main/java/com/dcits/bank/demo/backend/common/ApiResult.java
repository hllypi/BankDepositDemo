package com.dcits.bank.demo.backend.common;

import com.dcits.bank.demo.backend.enums.ResultCode;

public class ApiResult<T> {

    private int result_code;
    private String result_msg;
    private T data;

    private ApiResult(int result_code, String result_msg, T data) {
        this.result_code = result_code;
        this.result_msg = result_msg;
        this.data = data;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> ApiResult<T> fail(ResultCode code) {
        return new ApiResult<>(code.getCode(), code.getMessage(), null);
    }

    public static <T> ApiResult<T> fail(ResultCode code, String msg) {
        return new ApiResult<>(code.getCode(), msg, null);
    }

    public int getResult_code() { return result_code; }
    public void setResult_code(int result_code) { this.result_code = result_code; }
    public String getResult_msg() { return result_msg; }
    public void setResult_msg(String result_msg) { this.result_msg = result_msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
