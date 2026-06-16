package com.dcits.bank.demo.backend.advice;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.dto.AccountInfoResponse;
import com.github.houbb.sensitive.core.api.SensitiveUtil;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class SensitiveResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResult.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof ApiResult<?> apiResult)) {
            return body;
        }

        Object data = apiResult.getData();
        if (data instanceof AccountInfoResponse accountInfoResponse) {
            setDesensitizedData(apiResult, SensitiveUtil.desCopy(accountInfoResponse));
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private void setDesensitizedData(ApiResult<?> apiResult, AccountInfoResponse accountInfoResponse) {
        ((ApiResult<AccountInfoResponse>) apiResult).setData(accountInfoResponse);
    }
}
