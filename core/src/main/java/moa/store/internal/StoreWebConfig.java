package moa.store.internal;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class StoreWebConfig implements WebMvcConfigurer {

    private final StoreAccessInterceptor storeAccessInterceptor;

    StoreWebConfig(StoreAccessInterceptor storeAccessInterceptor) {
        this.storeAccessInterceptor = storeAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(storeAccessInterceptor)
                .addPathPatterns("/api/stores/*/**")
                // **는 경로 0개도 포함해 /api/stores/{id}에도 걸린다. store 자기 경로는 빼서 NOT_FOUND를 지킨다 (04 설계 1.7)
                .excludePathPatterns("/api/stores/*");
    }
}
