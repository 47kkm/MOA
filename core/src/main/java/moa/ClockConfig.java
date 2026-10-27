package moa;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * "오늘"을 만드는 곳은 이 빈 하나 (04 설계 D0-6). 테스트에서는 고정된 시계로 바꿔 넣는다.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
