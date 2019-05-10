/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.override.scan;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.OverrideConfigurationPropertiesScan;
import org.springframework.boot.test.autoconfigure.override.ExampleTestProperties;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for
 * {@link OverrideConfigurationPropertiesScan @OverrideConfigurationPropertiesScan} when
 * {@code enabled} is {@code false}.
 *
 * @author Madhura Bhave
 */
@RunWith(SpringRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@OverrideConfigurationPropertiesScan(enabled = false)
@Import(OverrideConfigurationPropertiesScanEnabledFalseIntegrationTests.TestConfiguration.class)
public class OverrideConfigurationPropertiesScanEnabledFalseIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void disabledConfigurationPropertiesScan() {
		ApplicationContext context = this.context;
		assertThat(context.getBean(ExampleTestProperties.class)).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean(ExampleProperties.class));
	}

	@Configuration
	@EnableConfigurationProperties(ExampleTestProperties.class)
	static class TestConfiguration {

	}

}
