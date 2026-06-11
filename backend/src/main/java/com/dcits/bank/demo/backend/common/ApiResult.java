package com.dcits.bank.demo.backend.common;

import com.dcits.bank.demo.backend.enums.ResultCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一响应体")
public class ApiResult<T> {

    @Schema(description = "结果码（0-成功）", example = "0")
    private int result_code;

    @Schema(description = "结果描述", example = "成功")
    private String result_msg;

    @Schema(description = "业务数据")
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
