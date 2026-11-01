package moa.shared.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import moa.shared.ErrorCode;
import moa.shared.MoaException;

// 모든 컨트롤러의 예외를 {code, message} 응답으로 바꾼다 (04 설계 0.6)
@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    // 주소나 파라미터의 숫자 자리에 글자 (/api/stores/abc …)
    @ExceptionHandler(TypeMismatchException.class)
    ResponseEntity<ErrorBody> handle(TypeMismatchException e) {
        return respond(ErrorCode.VALIDATION, "'" + e.getValue() + "' is not a valid value");
    }

    // 그 밖의 모든 예외
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorBody> handle(Exception e) {
        // Spring MVC가 직접 내는 4xx (없는 주소 404, 지원하지 않는 메서드 405 …)는 상태 코드를 그대로 둔다
        if (e instanceof ErrorResponse springError && springError.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = springError.getStatusCode();
            ErrorCode code = status.value() == 404 ? ErrorCode.NOT_FOUND : ErrorCode.VALIDATION;
            return ResponseEntity.status(status).body(new ErrorBody(code.name(), e.getMessage()));
        }
        // 서버 오류: 스택은 로그에만, 응답에는 코드만 (04 설계 0.6)
        log.error("unhandled exception", e);
        return respond(ErrorCode.INTERNAL_ERROR, "internal error");
    }

    private static ResponseEntity<ErrorBody> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(new ErrorBody(code.name(), message));
    }
}
