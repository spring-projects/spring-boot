/*
 * Copyright 2012-present the original author or authors.
 */
package org.springframework.boot.autoconfigure.usage;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class UsageAnalysisAutoConfigurationFailOnUnusedTests {

    @Test
    void failOnUnusedTriggersOnlyWhenUnusedPresent() throws Exception {
        Path out = Path.of("build", "boot-usage");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Sample.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.boot.usage.report.enabled=true",
                        "spring.boot.usage.report.fail-on-unused=true",
                        "spring.main.banner-mode=off"
                )
                .run()) {
            Path report = out.resolve("usage-report.json");
            assertThat(Files.exists(report)).isTrue();
            String json = Files.readString(report);
            if (json.contains("\"unusedStarters\": [\n    \"")) {
                // If there are any unused starters listed, unusedStarterFailure flag must be set
                assertThat(json).contains("unusedStarterFailure");
            }
        }
    }

    @Test
    void suggestionsSectionPresent() throws Exception {
        Path out = Path.of("build", "boot-usage");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Sample.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.boot.usage.report.enabled=true",
                        "spring.main.banner-mode=off"
                )
                .run()) {
            Path report = out.resolve("usage-report.json");
            assertThat(Files.exists(report)).isTrue();
            String json = Files.readString(report);
            assertThat(json).contains("\"suggestions\"");
            assertThat(json).contains("removeStarters");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Sample { }
}
