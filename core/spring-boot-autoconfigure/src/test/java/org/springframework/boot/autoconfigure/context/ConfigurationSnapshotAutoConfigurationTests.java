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

package org.springframework.boot.autoconfigure.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationSnapshotAutoConfiguration}.
 *
 * @author Your Name
 * @since 3.2.0
 */
class ConfigurationSnapshotAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
            AutoConfigurations.of(ConfigurationSnapshotAutoConfiguration.class));

    @Test
    void configurationSnapshotAutoConfigurationIsDisabledByDefault() {
        this.contextRunner.run((context) -> {
            assertThat(context.getBeansOfType(ConfigurationSnapshotListener.class)).isEmpty();
        });
    }

    @Test
    void configurationSnapshotAutoConfigurationIsEnabledWhenPropertyIsSetToTrue() {
        this.contextRunner.withPropertyValues("spring.config-snapshot.enabled=true")
                .run((context) -> {
                    assertThat(context.getBeansOfType(ConfigurationSnapshotListener.class)).isNotEmpty();
                });
    }

    @Test
    void configurationSnapshotAutoConfigurationIsDisabledWhenPropertyIsSetToFalse() {
        this.contextRunner.withPropertyValues("spring.config-snapshot.enabled=false")
                .run((context) -> {
                    assertThat(context.getBeansOfType(ConfigurationSnapshotListener.class)).isEmpty();
                });
    }

    @Test
    void configurationSnapshotPropertiesAreBoundCorrectly() {
        this.contextRunner.withPropertyValues(
                "spring.config-snapshot.enabled=true",
                "spring.config-snapshot.log-to=FILE",
                "spring.config-snapshot.include=spring.datasource.url,spring.cache.enabled")
                .run((context) -> {
                    ConfigurationSnapshotProperties properties = context.getBean(ConfigurationSnapshotProperties.class);
                    assertThat(properties.isEnabled()).isTrue();
                    assertThat(properties.getLogTo()).isEqualTo(ConfigurationSnapshotProperties.LogTo.FILE);
                    assertThat(properties.getInclude()).containsExactly("spring.datasource.url", "spring.cache.enabled");
                });
    }
}
