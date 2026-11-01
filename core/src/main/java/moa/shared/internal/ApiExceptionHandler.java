package moa.shared.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import moa.shared.ErrorCode;
import moa.shared.MoaException;

// 모든 컨트롤러의 예외를 {code, message} 응답으로 바꾼다 (04 설계 0.6)
@RestControllerAdvice
class ApiExceptionHandler {

    // 응용 서비스의 업무 규칙 위반 (NOT_FOUND, STORE_ALREADY_EXISTS …)
    @ExceptionHandler(MoaException.class)
    ResponseEntity<ErrorBody> handle(MoaException e) {
        return respond(e.getCode(), e.getMessage());
    }

    // 애그리거트의 입력 규칙 위반 (이름 비움, 50자 초과 …)
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorBody> handle(IllegalArgumentException e) {
        return respond(ErrorCode.VALIDATION, e.getMessage());
    }

    // 요청 JSON을 읽지 못함 (문법 오류, 목록에 없는 업종 값 …)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> handle(HttpMessageNotReadableException e) {
        return respond(ErrorCode.VALIDATION, "request body is not readable");
    }

    private static ResponseEntity<ErrorBody> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(new ErrorBody(code.name(), message));
    }
}
