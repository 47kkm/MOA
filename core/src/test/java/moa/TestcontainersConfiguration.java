package moa;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;

/**
 * 테스트용 MySQL. docker-compose.yml과 같은 이미지를 Docker로 잠깐 띄운다.
 * root로 접속해야 Flyway가 스키마 7개를 만들 수 있다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysql() {
        return new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("moa")
                .withUsername("root")
                .withPassword("moa");
    }
}
