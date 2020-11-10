package org.tdf.sunflower.controller;

/**
 * json format:
 * <p>
 * {
 * "code": 500,
 * "data": "data",
 * "message": "success"
 * }
 */
public class Response<T> {
    private int code;
    private T data;
    private String message;

    private Response(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static <T> Response<T> newSuccessFul(T data) {
        return new Response<>(Code.SUCCESS.code, data, Code.SUCCESS.message);
    }

    public static <T> Response<T> newFailed(Code code) {
        return new Response<>(code.code, null, code.message);
    }

    public static <T> Response<T> newFailed(Code code, String reason) {
        return new Response<>(code.code, null, reason);
    }

    public int getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public static enum Code {
        SUCCESS(200, "success"),

        INTERNAL_ERROR(500, "internal error");

        public final int code;

        public final String message;

        Code(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
