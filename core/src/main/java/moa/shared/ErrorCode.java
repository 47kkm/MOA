package moa.shared;

import org.springframework.http.HttpStatus;

// 오류 코드 전체 목록 (04 설계 0.6). 모듈을 새로 만들면 그 모듈의 코드를 여기에 더한다
public enum ErrorCode {

    VALIDATION(HttpStatus.BAD_REQUEST),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    STORE_NOT_FOUND(HttpStatus.NOT_FOUND),
    STORE_ALREADY_EXISTS(HttpStatus.CONFLICT);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
