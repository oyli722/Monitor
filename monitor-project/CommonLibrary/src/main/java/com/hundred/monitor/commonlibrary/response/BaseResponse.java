package com.hundred.monitor.commonlibrary.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    /**
     * 响应码
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
     * 时间戳
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();

    /**
     * 常见响应码常量
     */
    public static final Integer SUCCESS_CODE = 200;
    public static final Integer BAD_REQUEST_CODE = 400;
    public static final Integer UNAUTHORIZED_CODE = 401;
    public static final Integer FORBIDDEN_CODE = 403;
    public static final Integer NOT_FOUND_CODE = 404;
    public static final Integer INTERNAL_SERVER_ERROR_CODE = 500;

    /**
     * 默认成功消息
     */
    public static final String DEFAULT_SUCCESS_MESSAGE = "操作成功";

    /**
     * 成功响应（无数据）
     */
    public static <T> BaseResponse<T> success() {
        return success(DEFAULT_SUCCESS_MESSAGE, null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> BaseResponse<T> success(T data) {
        return success(DEFAULT_SUCCESS_MESSAGE, data);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .code(SUCCESS_CODE)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> BaseResponse<T> error(Integer code, String message) {
        return error(code, message, null);
    }

    /**
     * 失败响应（带数据）
     */
    public static <T> BaseResponse<T> error(Integer code, String message, T data) {
        return BaseResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 失败响应（默认错误码500）
     */
    public static <T> BaseResponse<T> error(String message) {
        return error(INTERNAL_SERVER_ERROR_CODE, message);
    }

    /**
     * 常见错误响应快捷方法
     */
    public static <T> BaseResponse<T> badRequest(String message) {
        return error(BAD_REQUEST_CODE, message);
    }

    public static <T> BaseResponse<T> unauthorized(String message) {
        return error(UNAUTHORIZED_CODE, message);
    }

    public static <T> BaseResponse<T> forbidden(String message) {
        return error(FORBIDDEN_CODE, message);
    }

    public static <T> BaseResponse<T> notFound(String message) {
        return error(NOT_FOUND_CODE, message);
    }

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return SUCCESS_CODE.equals(this.code);
    }

    /**
     * 判断响应是否失败
     */
    public boolean isError() {
        return !isSuccess();
    }
}
