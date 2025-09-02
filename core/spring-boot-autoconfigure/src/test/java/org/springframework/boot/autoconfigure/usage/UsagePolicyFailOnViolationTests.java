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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import java.time.Duration;

class UsagePolicyFailOnViolationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    PropertyPlaceholderAutoConfiguration.class,
                    UsageAnalysisAutoConfiguration.class,
                    PolicyConfig.class))
            .withPropertyValues(
                    "spring.boot.usage.report.enabled=true",
                    "spring.boot.usage.report.policies.fail-on-violation=true");

    @Test
    void contextFailsOnViolation() {
        this.runner.run(context -> {
        assertThatThrownBy(() -> context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], context, Duration.ZERO)))
            .hasMessageContaining("Policy violation prevented startup");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class PolicyConfig {
        @Bean
        UsagePolicy samplePolicy() {
            return (structured, props) -> new UsagePolicy.PolicyResult(List.of(), List.of("Always fail policy"));
        }
    }
}
