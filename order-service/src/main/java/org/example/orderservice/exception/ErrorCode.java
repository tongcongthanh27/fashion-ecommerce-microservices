package org.example.orderservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    PRODUCT_NOT_FOUND(1001,"Không tìm thấy thông tin sản phẩm", HttpStatus.NOT_FOUND),
    KO_DU_SO_LUONG(1001,"San pham ko du so luong", HttpStatus.NOT_FOUND)

    ;
    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
