package org.springframework.boot.actuate.autoconfigure.usage;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.usage.UsageEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.usage.UsageAnalysisAutoConfiguration;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class UsageEndpointAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
                    "management.endpoint.bootusage.enabled=true",
                    "management.endpoints.web.exposure.include=bootusage");

    @Test
    void disabledPropertyDoesNotCreateEndpoint() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration.class,
            PropertyPlaceholderAutoConfiguration.class,
            EndpointAutoConfiguration.class,
            JacksonEndpointAutoConfiguration.class,
                    WebEndpointAutoConfiguration.class,
                    ManagementContextAutoConfiguration.class,
            UsageAnalysisAutoConfiguration.class,
            UsageEndpointAutoConfiguration.class))
            .withPropertyValues("management.endpoints.web.exposure.include=bootusage","management.endpoint.bootusage.enabled=true")
            .run((context) -> assertThat(context).doesNotHaveBean(UsageEndpoint.class));
    }

    @Test
    void enabledCreatesEndpoint() {
        this.contextRunner.run((context) -> {
            boolean endpoint = context.containsBean("usageEndpoint");
            boolean service = context.getBeansOfType(org.springframework.boot.autoconfigure.usage.UsageReportService.class).size() == 1;
            if (!endpoint) {
                org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport report = org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.get(context.getBeanFactory());
                System.out.println("=== Diagnostics: UsageEndpointAutoConfiguration (FULL) ===");
                System.out.println("servicePresent=" + service);
                System.out.println("properties.enabled=" + context.getEnvironment().getProperty("spring.boot.usage.report.enabled"));
                if (report != null) {
                    report.getConditionAndOutcomesBySource().forEach((k,v) -> {
                        System.out.println(k + " matched=" + v.isFullMatch());
                        v.forEach(outcome -> System.out.println("  - " + outcome));
                    });
                }
            }
            assertThat(endpoint).as("UsageEndpoint bean should be present").isTrue();
        });
    }
}
