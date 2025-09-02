package org.springframework.boot.actuate.autoconfigure.usage;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.usage.UsageAnalysisAutoConfiguration;
import org.springframework.boot.autoconfigure.usage.UsageReportService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class UsageAnalysisAutoConfigurationActivationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class,
                    UsageAnalysisAutoConfiguration.class))
            .withPropertyValues("spring.boot.usage.report.enabled=true");

    @Test
    void serviceBeanCreatedWhenEnabledPropertyTrue() {
        this.runner.run(ctx -> assertThat(ctx).hasSingleBean(UsageReportService.class));
    }

    @Test
    void serviceBeanAbsentWhenPropertyMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ConfigurationPropertiesAutoConfiguration.class,
                        PropertyPlaceholderAutoConfiguration.class,
                        UsageAnalysisAutoConfiguration.class))
                .run(ctx -> assertThat(ctx).doesNotHaveBean(UsageReportService.class));
    }
}