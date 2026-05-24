package com.xiaozhanke.deploy.exception;

import com.xiaozhanke.deploy.model.response.RestErrorResponse;
import com.xiaozhanke.deploy.model.response.RestErrorResponse.FieldViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author xiaozhanke
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public GlobalExceptionHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 处理资源未找到异常
     *
     * @param e 资源未找到异常
     * @return 错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<RestErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * 处理资源冲突异常
     *
     * @param e 资源冲突异常
     * @return 错误响应
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<RestErrorResponse> handleDuplicateResource(DuplicateResourceException e) {
        log.warn("资源冲突: {}", e.getMessage());
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.CONFLICT.value(), "RESOURCE_CONFLICT", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * 处理无效操作异常
     *
     * @param e 无效操作异常
     * @return 错误响应
     */
    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<RestErrorResponse> handleInvalidOperation(InvalidOperationException e) {
        log.warn("无效操作: {}", e.getMessage());
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_OPERATION", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RestErrorResponse> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        RestErrorResponse response = RestErrorResponse.of(e.getCode(), "BUSINESS_ERROR", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理参数验证异常 (@RequestBody)
     *
     * @param e 参数验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<FieldViolation> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        log.warn("参数验证异常: {}", details);
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", "请求参数无效", details);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理参数绑定异常 (form data)
     *
     * @param e 参数绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<RestErrorResponse> handleBindException(BindException e) {
        List<FieldViolation> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        log.warn("参数绑定异常: {}", details);
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", "参数绑定失败", details);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理无法读取 HTTP 请求体异常
     *
     * @param e 无法读取 HTTP 请求体异常
     * @return 错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("无法读取 HTTP 请求体: {}", e.getMessage());
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST_BODY", "请求体不能为空或格式错误");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有其他未捕获的异常
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestErrorResponse> handleException(Exception e) {
        log.error("服务器内部错误", e);
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * 处理 WebSocket 中的参数验证异常
     *
     * @param e              参数验证异常
     * @param headerAccessor 消息头部访问器
     */
    @MessageExceptionHandler(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class)
    public void handleWebSocketValidationException(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException e, SimpMessageHeaderAccessor headerAccessor) {
        String username = (headerAccessor.getUser() != null) ? headerAccessor.getUser().getName() : "unknown";
        BindingResult bindingResult = e.getBindingResult();
        RestErrorResponse response;
        if (bindingResult != null) {
            List<FieldViolation> details = bindingResult.getFieldErrors().stream()
                    .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                    .collect(Collectors.toList());
            log.warn("WebSocket 参数验证异常, 用户 [{}], 错误详情: {}", username, details);
            response = RestErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", "WebSocket 消息参数校验失败", details);
        } else {
            log.error("WebSocket 参数验证异常, 用户 [{}], 但无法获取 BindingResult", username, e);
            response = RestErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "INVALID_ARGUMENT", "WebSocket 消息参数校验失败，但无法获取详细信息");
        }
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", response);
    }

    /**
     * 处理 WebSocket 中的所有其他异常
     *
     * @param e              异常
     * @param headerAccessor 消息头部访问器
     */
    @MessageExceptionHandler(Exception.class)
    public void handleWebSocketException(Exception e, SimpMessageHeaderAccessor headerAccessor) {
        String username = (headerAccessor.getUser() != null) ? headerAccessor.getUser().getName() : "unknown";
        log.error("WebSocket 内部异常, 用户 [{}]: {}", username, e.getMessage(), e);
        RestErrorResponse response = RestErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "WebSocket 通信发生内部错误");
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", response);
    }

} 