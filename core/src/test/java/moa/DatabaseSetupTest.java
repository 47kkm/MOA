package moa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseSetupTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("앱 기동 - Flyway가 스키마 7개와 발행 기록 테이블을 만든다")
    void schemasCreated() {
        List<String> schemas = jdbc.queryForList(
                "SELECT schema_name FROM information_schema.schemata", String.class);

        assertThat(schemas).contains("moa", "store", "category", "sales", "expense", "budget", "report");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM moa.EVENT_PUBLICATION", Integer.class)).isZero();
    }
}
