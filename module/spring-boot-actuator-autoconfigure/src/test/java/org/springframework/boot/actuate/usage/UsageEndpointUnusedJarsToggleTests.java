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

class UsageEndpointUnusedJarsToggleTests {

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
                    "spring.boot.usage.report.detect-unused-jars=false",
                    "management.endpoints.web.exposure.include=bootusage");

    @Test
    void unusedJarsOmittedWhenDisabled() {
        this.runner.run(ctx -> {
            UsageEndpoint endpoint = ctx.getBean(UsageEndpoint.class);
            Map<String,Object> body = endpoint.usage(null);
            assertThat(body).doesNotContainKey("unusedJars");
        });
    }
}
