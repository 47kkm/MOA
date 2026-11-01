package moa.shared;

// 응용 서비스가 업무 규칙 위반(404·409)을 알릴 때 던진다. 공통 핸들러가 code로 HTTP 응답을 만든다 (04 설계 0.6)
public class MoaException extends RuntimeException {

    private final ErrorCode code;

    public MoaException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
