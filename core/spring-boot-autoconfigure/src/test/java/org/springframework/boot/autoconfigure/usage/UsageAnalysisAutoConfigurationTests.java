/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.usage;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UsageAnalysisAutoConfiguration}.
 */
class UsageAnalysisAutoConfigurationTests {

    @Test
    void generatesJsonReportWhenEnabled() throws Exception {
        Path out = Path.of("build", "boot-usage");
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Sample.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.boot.usage.report.enabled=true",
                        "spring.main.banner-mode=off")
                .run()) {
            Path report = out.resolve("usage-report.json");
            assertThat(Files.exists(report)).isTrue();
            String json = Files.readString(report);
            assertThat(json).contains("appliedAutoConfigurations");
            assertThat(json).contains("beanOrigins");
            assertThat(json).contains("declaredStarters");
            assertThat(json).contains("usedStarters");
            assertThat(json).contains("unusedStarters");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class Sample { }
}
