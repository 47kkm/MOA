package moa.store.internal;

import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import moa.shared.ErrorCode;
import moa.shared.MoaException;

// 다른 모듈의 컨트롤러에 닿기 전에 "그 매장이 있나"만 본다. Phase 2 로그인 검사가 들어갈 자리 (04 설계 1.2, D1-5)
@Component
class StoreAccessInterceptor implements HandlerInterceptor {

    private static final PathPattern STORE_PATH = PathPatternParser.defaultInstance.parse("/api/stores/{storeId}/**");

    private final StoreRepository stores;

    StoreAccessInterceptor(StoreRepository stores) {
        this.stores = stores;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        PathPattern.PathMatchInfo match = STORE_PATH.matchAndExtract(PathContainer.parsePath(request.getRequestURI()));
        if (match == null) {
            return true;
        }
        Long storeId = Long.valueOf(match.getUriVariables().get("storeId"));
        if (!stores.existsById(storeId)) {
            throw new MoaException(ErrorCode.STORE_NOT_FOUND, "store not found: " + storeId);
        }
        return true;
    }
}
