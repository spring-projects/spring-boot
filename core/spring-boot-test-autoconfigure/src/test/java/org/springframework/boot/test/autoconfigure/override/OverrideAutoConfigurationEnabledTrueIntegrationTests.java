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

package org.springframework.boot.test.autoconfigure.override;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.test.autoconfigure.ExampleTestConfig;
import org.springframework.boot.test.autoconfigure.OverrideAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OverrideAutoConfiguration @OverrideAutoConfiguration} when
 * {@code enabled} is {@code true}.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@OverrideAutoConfiguration(enabled = true)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@ImportAutoConfiguration(ExampleTestConfig.class)
class OverrideAutoConfigurationEnabledTrueIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void autoConfiguredContext() {
		ApplicationContext context = this.context;
		assertThat(context.getBean(OverrideAutoConfigurationSpringBootApplication.class)).isNotNull();
		assertThat(context.getBean(ConfigurationPropertiesBindingPostProcessor.class)).isNotNull();
	}

}
