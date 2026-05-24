package com.xiaozhanke.deploy.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

/**
 * REST 接口统一错误响应体
 *
 * @author xiaozhanke
 */
@Schema(description = "REST 接口统一错误响应体")
@Getter
public class RestErrorResponse {

    @Schema(description = "错误详情的容器对象")
    private final RestError error;

    private RestErrorResponse(RestError error) {
        this.error = error;
    }

    /**
     * 创建一个包含详细错误列表的错误响应 (例如，用于参数校验)
     *
     * @param code    HTTP 状态码
     * @param status  规范化错误码
     * @param message 概括性消息
     * @param details 字段校验详情列表
     * @return 一个新的 RestErrorResponse 实例
     */
    public static RestErrorResponse of(int code, String status, String message, List<FieldViolation> details) {
        RestError restError = new RestError(code, status, message, details);
        return new RestErrorResponse(restError);
    }

    /**
     * 创建一个不包含详细错误列表的错误响应
     *
     * @param code    HTTP 状态码
     * @param status  规范化错误码
     * @param message 概括性消息
     * @return 一个新的 RestErrorResponse 实例
     */
    public static RestErrorResponse of(int code, String status, String message) {
        return of(code, status, message, null);
    }

    /**
     * 核心错误体，包含了所有关于错误的描述信息。
     */
    @Schema(description = "核心错误体")
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RestError {

        @Schema(description = "HTTP 状态码", example = "400")
        private final int code;

        @Schema(description = "规范化的、可编程的错误状态码", example = "INVALID_ARGUMENT")
        private final String status;

        @Schema(description = "可读的错误消息", example = "请求参数无效")
        private final String message;

        @Schema(description = "可读的错误详情列表")
        private final List<FieldViolation> details;

        public RestError(int code, String status, String message, List<FieldViolation> details) {
            this.code = code;
            this.status = status;
            this.message = message;
            this.details = details;
        }
    }

    /**
     * 描述一个具体的字段校验错误。
     * 当发生参数校验失败 (INVALID_ARGUMENT) 时，details 列表中会包含此类型的对象。
     */
    @Schema(description = "字段校验错误详情")
    @Getter
    public static class FieldViolation {

        @Schema(description = "校验失败的字段名", example = "username")
        private final String field;

        @Schema(description = "该字段错误的具体描述", example = "用户名不能为空")
        private final String description;

        public FieldViolation(String field, String description) {
            this.field = field;
            this.description = description;
        }
    }
}