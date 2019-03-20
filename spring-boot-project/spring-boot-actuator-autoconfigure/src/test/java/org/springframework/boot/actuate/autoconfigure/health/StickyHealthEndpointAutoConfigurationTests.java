package org.springframework.boot.actuate.autoconfigure.health;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StickyHealthIndicatorDecoratorBeanPostProcessor}.
 *
 * @author Vladislav Fefelov
 * @since 20.03.2019
 */
public class StickyHealthEndpointAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(HealthIndicatorAutoConfiguration.class,
                HealthEndpointAutoConfiguration.class));

    @Test
    public void healthEndpointMergeRegularAndReactive() {
        this.contextRunner
            .withPropertyValues("management.health.status.sticky[0]=simple")
            .withUserConfiguration(HealthIndicatorConfiguration.class)
            .run((context) -> {
                AtomicReference<Health> indicatorState = context.getBean("simpleHealthIndicatorState",
                    AtomicReference.class);
                HealthIndicator indicator = context.getBean("simpleHealthIndicator",
                    HealthIndicator.class);

                Health initialDownState = Health.down().build();
                indicatorState.set(initialDownState);

                assertThat(indicator.health()).isSameAs(initialDownState);

                Health upState = Health.up().build();
                indicatorState.set(upState);

                assertThat(indicator.health()).isSameAs(upState);

                Health nextDownState = Health.down()
                    .withDetail("custom", "detail")
                    .build();
                indicatorState.set(nextDownState);

                assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
                assertThat(indicator.health().getDetails().get("originalStatus")).isEqualTo(Status.DOWN);
                assertThat(indicator.health().getDetails().get("originalDetails")).isEqualTo(nextDownState.getDetails());
            });
    }

    @Configuration
    static class HealthIndicatorConfiguration {

        @Bean
        public AtomicReference<Health> simpleHealthIndicatorState() {
            return new AtomicReference<>(Health.up().build());
        }

        @Bean
        public HealthIndicator simpleHealthIndicator() {
            return simpleHealthIndicatorState()::get;
        }

    }


}
