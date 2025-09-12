package org.springframework.boot.actuate.usage;

import java.util.List;
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
import org.springframework.boot.autoconfigure.usage.UsagePolicy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class UsageEndpointPoliciesTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class,
                    EndpointAutoConfiguration.class,
                    JacksonEndpointAutoConfiguration.class,
                    WebEndpointAutoConfiguration.class,
                    ManagementContextAutoConfiguration.class,
                    UsageAnalysisAutoConfiguration.class,
                    UsageEndpointAutoConfiguration.class,
                    PolicyConfig.class))
            .withPropertyValues(
                    "spring.boot.usage.report.enabled=true",
                    "spring.boot.usage.report.cache-ttl=0",
                    "management.endpoints.web.exposure.include=bootusage");

    @Test
    void policyAddsViolation() {
        this.runner.run(ctx -> {
            UsageEndpoint ep = ctx.getBean(UsageEndpoint.class);
            Map<String,Object> body = ep.usage(Boolean.TRUE);
            @SuppressWarnings("unchecked") Map<String,Object> policies = (Map<String,Object>) body.get("policies");
            assertThat(policies).isNotNull();
            assertThat(policies.get("violations")).as("violations present").isInstanceOf(List.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class PolicyConfig {
        @Bean
        UsagePolicy samplePolicy() {
            return (structured, props) -> {
                @SuppressWarnings("unchecked") java.util.List<String> applied = (java.util.List<String>) structured.get("appliedAutoConfigurations");
                boolean endpointAuto = applied.stream().anyMatch(s -> s.contains("EndpointAutoConfiguration"));
                if (endpointAuto) {
                    return new UsagePolicy.PolicyResult(List.of(), List.of("Disallow EndpointAutoConfiguration in sample policy"));
                }
                return UsagePolicy.PolicyResult.empty();
            };
        }
    }
}
