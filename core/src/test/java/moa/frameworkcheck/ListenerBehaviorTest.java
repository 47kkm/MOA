package moa.frameworkcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import moa.TestcontainersConfiguration;

/**
 * 기능 테스트가 아니라 설계 전제의 확인 (04 설계 0.3 "리스너가 실패하면", 0.7 "프레임워크 확인").
 * MOA의 리스너 조합(@TransactionalEventListener + REQUIRES_NEW)이 문서대로 움직이는지 본다.
 * 하나라도 실패하면 0.3과 D0-3을 고친다.
 */
@SpringBootTest
@Import({ TestcontainersConfiguration.class, ListenerBehaviorTest.Fixture.class })
class ListenerBehaviorTest {

    @Autowired
    Publisher publisher;

    @Autowired
    FlakyListener listener;

    @Autowired
    IncompleteEventPublications incompletePublications;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM moa.EVENT_PUBLICATION");
        listener.shouldFail(false);
        listener.handled().clear();
    }

    @Test
    @DisplayName("리스너 확인 - 성공한 리스너는 발행하는 쪽 메서드가 돌아오기 전에 끝나 있다 (동기)")
    void runsBeforeCallerReturns() {
        publisher.publish("a");

        assertThat(listener.handled()).containsExactly("a");
        assertThat(incompleteCount()).isZero();
    }

    @Test
    @DisplayName("리스너 확인 - 커밋 후 리스너의 예외는 발행하는 쪽으로 전파되지 않는다")
    void exceptionNotPropagated() {
        listener.shouldFail(true);

        assertThatCode(() -> publisher.publish("b")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("리스너 확인 - 실패한 리스너의 발행 기록은 미완료로 남는다")
    void failedPublicationStaysIncomplete() {
        listener.shouldFail(true);

        publisher.publish("c");

        assertThat(incompleteCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("리스너 확인 - 미완료 발행을 재발행하면 처리되고 완료로 바뀐다")
    void resubmitCompletes() {
        listener.shouldFail(true);
        publisher.publish("d");
        listener.shouldFail(false);

        incompletePublications.resubmitIncompletePublications(publication -> true);

        assertThat(listener.handled()).containsExactly("d");
        assertThat(incompleteCount()).isZero();
    }

    private int incompleteCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM moa.EVENT_PUBLICATION WHERE COMPLETION_DATE IS NULL", Integer.class);
    }

    record CheckEvent(String id) {
    }

    static class Publisher {

        private final ApplicationEventPublisher events;

        Publisher(ApplicationEventPublisher events) {
            this.events = events;
        }

        @Transactional
        public void publish(String id) {
            events.publishEvent(new CheckEvent(id));
        }
    }

    // @Transactional 때문에 이 빈은 프록시다. 프록시의 필드는 비어 있으므로 상태는 메서드로만 만진다
    static class FlakyListener {

        private volatile boolean fail;
        private final List<String> handled = new CopyOnWriteArrayList<>();

        public void shouldFail(boolean fail) {
            this.fail = fail;
        }

        public List<String> handled() {
            return handled;
        }

        @TransactionalEventListener
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void on(CheckEvent event) {
            if (fail) {
                throw new IllegalStateException("의도한 실패: " + event.id());
            }
            handled.add(event.id());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixture {

        @Bean
        Publisher publisher(ApplicationEventPublisher events) {
            return new Publisher(events);
        }

        @Bean
        FlakyListener flakyListener() {
            return new FlakyListener();
        }
    }
}
