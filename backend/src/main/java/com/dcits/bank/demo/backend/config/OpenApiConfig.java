package com.dcits.bank.demo.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人活期存款核心账务系统")
                        .version("V1.0")
                        .description("银行个人活期存款账户全生命周期管理，涵盖开户、存款、取款、转账、结息等核心功能。")
                        .contact(new Contact().name("DCITS")));
    }

    /**
     * 为所有 /api/** 接口自动添加 X-Auth-Token 请求头参数，
     * 方便在 Swagger UI 中统一填写 Token 后测试。
     */
    @Bean
    public OperationCustomizer globalAuthHeader() {
        return (operation, handlerMethod) -> {
            Parameter tokenHeader = new HeaderParameter()
                    .name("X-Auth-Token")
                    .description("鉴权Token（固定SHA-256值）")
                    .required(true)
                    .example("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
            operation.addParametersItem(tokenHeader);
            return operation;
        };
    }
}
