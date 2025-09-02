package org.springframework.boot.actuate.usage;

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

class UsageEndpointOriginsToggleTests {

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
                    "spring.boot.usage.report.include-origins=false",
                    "management.endpoints.web.exposure.include=bootusage");

    @Test
    void beanOriginsOmittedWhenDisabled() {
        this.runner.run(ctx -> {
            UsageEndpoint endpoint = ctx.getBean(UsageEndpoint.class);
            @SuppressWarnings("unchecked") Map<String,Object> body = endpoint.usage(null);
            System.out.println("UsageEndpointOriginsToggleTests body keys=" + body.keySet());
            assertThat(body).doesNotContainKey("beanOrigins");
        });
    }
}
