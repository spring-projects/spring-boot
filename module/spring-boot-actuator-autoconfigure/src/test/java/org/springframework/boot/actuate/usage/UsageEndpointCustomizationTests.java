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
import org.springframework.boot.autoconfigure.usage.UsageReportCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class UsageEndpointCustomizationTests {

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
                    CustomizerConfig.class))
            .withPropertyValues(
                    "spring.boot.usage.report.enabled=true",
                    "spring.boot.usage.report.cache-ttl=0",
                    "spring.boot.usage.report.include-origins=false",
                    "spring.boot.usage.report.detect-unused-jars=true",
                    "management.endpoints.web.exposure.include=bootusage");

    @Test
    void customizerInvokedAndFeaturesPresent() {
        this.runner.run(ctx -> {
            UsageEndpoint ep = ctx.getBean(UsageEndpoint.class);
            @SuppressWarnings("unchecked") Map<String,Object> body = ep.usage(Boolean.TRUE);
            System.out.println("Customization full body=" + body);
            assertThat(body.get("customized")).isEqualTo(Boolean.TRUE);
            @SuppressWarnings("unchecked") Map<String,Object> suggestions = (Map<String,Object>) body.get("suggestions");
            assertThat(suggestions).isNotNull();
            @SuppressWarnings("unchecked") Map<String,Object> confidence = (Map<String,Object>) suggestions.get("confidence");
            assertThat(confidence).isNotNull();
            assertThat(suggestions).containsKey("unusedJars");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomizerConfig {
        @Bean
        UsageReportCustomizer markerCustomizer() {
            return (structured, props) -> structured.put("customized", Boolean.TRUE);
        }
    }
}
