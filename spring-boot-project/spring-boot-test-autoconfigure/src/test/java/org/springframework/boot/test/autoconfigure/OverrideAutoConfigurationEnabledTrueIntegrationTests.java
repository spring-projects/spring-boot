/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OverrideAutoConfiguration} when {@code enabled} is
 * {@code true}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@OverrideAutoConfiguration(enabled = true)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@ImportAutoConfiguration(ExampleTestConfig.class)
public class OverrideAutoConfigurationEnabledTrueIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void autoConfiguredContext() {
		ApplicationContext context = this.context;
		assertThat(context.getBean(ExampleSpringBootApplication.class)).isNotNull();
		assertThat(context.getBean(ConfigurationPropertiesBindingPostProcessor.class))
				.isNotNull();
	}

}
