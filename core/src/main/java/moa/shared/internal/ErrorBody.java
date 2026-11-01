package moa.shared.internal;

// 오류 응답 본문. 상태 코드는 HTTP 응답에 실리므로 본문에 status는 넣지 않는다 (04 설계 D0-5)
record ErrorBody(String code, String message) {
}
