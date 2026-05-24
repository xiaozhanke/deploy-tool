package com.xiaozhanke.deploy.config;

import com.xiaozhanke.deploy.model.response.RestErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * OpenAPI 配置
 *
 * @author xiaozhanke
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "${spring.application.name}", description = "管理端部署工具后端 API", version = "${spring.application.version}"),
        security = @SecurityRequirement(name = "Bearer Authentication"))
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    /**
     * 自定义 OpenAPI 规范
     */
    @Bean
    public OpenApiCustomizer errorResponseCustomizer() {
        return openApi -> {
            // 统一定义错误响应结构，避免每个 API 需要手动编写 @ApiResponse
            // 主动解析 RestErrorResponse
            ResolvedSchema resolvedSchema = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(RestErrorResponse.class));
            // 手动注册到 OpenAPI 的 components/schemas
            if (resolvedSchema.schema != null) {
                openApi.getComponents().addSchemas(resolvedSchema.schema.getName(), resolvedSchema.schema);
                if (resolvedSchema.referencedSchemas != null) {
                    resolvedSchema.referencedSchemas.forEach((key, schema) ->
                            openApi.getComponents().addSchemas(key, schema));
                }
            }
            // 定义 400 错误响应的标准格式
            ApiResponse badRequestResponse = createErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", "请求参数无效");
            // 定义 401 错误响应的标准格式
            ApiResponse unauthorizedResponse = createErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "用户未认证");
            // 定义 403 错误响应的标准格式
            ApiResponse forbiddenResponse = createErrorResponse(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "用户无权限");
            // 定义 404 错误响应的标准格式
            ApiResponse notFoundResponse = createErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "请求的资源不存在");
            // 定义 500 错误响应的标准格式
            ApiResponse internalErrorResponse = createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "服务器内部错误");
            // 遍历所有路径，自动添加响应示例
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                responses.addApiResponse(String.valueOf(HttpStatus.BAD_REQUEST.value()), badRequestResponse);
                responses.addApiResponse(String.valueOf(HttpStatus.UNAUTHORIZED.value()), unauthorizedResponse);
                responses.addApiResponse(String.valueOf(HttpStatus.FORBIDDEN.value()), forbiddenResponse);
                responses.addApiResponse(String.valueOf(HttpStatus.NOT_FOUND.value()), notFoundResponse);
                responses.addApiResponse(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), internalErrorResponse);
            }));
        };
    }

    /**
     * Rest 接口分组
     */
    @Bean
    public GroupedOpenApi restOpenApi() {
        String[] packagesToScan = {"com.xiaozhanke.deploy.controller"};
        return GroupedOpenApi.builder().group("Rest 接口").packagesToScan(packagesToScan).addOpenApiCustomizer(errorResponseCustomizer()).build();
    }

    /**
     * Actuator 接口分组
     */
    @Bean
    public GroupedOpenApi actuatorOpenApi() {
        String[] paths = {"/actuator/**"};
        return GroupedOpenApi.builder().group("Actuator 接口").pathsToMatch(paths).addOpenApiCustomizer(errorResponseCustomizer()).build();
    }

    /**
     * 创建标准化的 ApiResponse 对象
     */
    private ApiResponse createErrorResponse(HttpStatus status, String errorCode, String message) {
        Schema<?> schema = new Schema<>().$ref("RestErrorResponse");
        return new ApiResponse()
                .description(status.getReasonPhrase())
                .content(new Content()
                        .addMediaType(MediaType.APPLICATION_JSON_VALUE,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(schema)
                                        .example(RestErrorResponse.of(status.value(), errorCode, message))));
    }

}