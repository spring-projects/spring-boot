package org.springframework.boot.actuate.usage;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.usage.UsageEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.usage.UsageAnalysisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class UsageEndpointCachingTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class,
                    EndpointAutoConfiguration.class,
                    JacksonEndpointAutoConfiguration.class,
                    WebEndpointAutoConfiguration.class,
                    ManagementContextAutoConfiguration.class,
                    UsageAnalysisAutoConfiguration.class,
                    UsageEndpointAutoConfiguration.class))
            .withPropertyValues(
                    "spring.boot.usage.report.enabled=true",
                    "spring.boot.usage.report.cache-ttl=5m",
                    "management.endpoints.web.exposure.include=bootusage");

    @Test
    void forceBypassesCache() {
        this.runner.run(ctx -> {
            UsageEndpoint endpoint = ctx.getBean(UsageEndpoint.class);
            Map<String,Object> first = endpoint.usage(null);
            Map<String,Object> second = endpoint.usage(null);
            assertThat(first.get("timestamp")).isEqualTo(second.get("timestamp"));
            Map<String,Object> forced = endpoint.usage(Boolean.TRUE);
            assertThat(forced.get("timestamp")).isNotEqualTo(first.get("timestamp"));
        });
    }
}
