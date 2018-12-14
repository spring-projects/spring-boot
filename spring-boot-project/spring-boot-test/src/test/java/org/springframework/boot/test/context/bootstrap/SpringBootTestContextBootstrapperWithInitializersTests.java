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

package org.springframework.boot.test.context.bootstrap;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.context.bootstrap.SpringBootTestContextBootstrapperWithInitializersTests.CustomInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SpringBootTestContextBootstrapper} with
 * {@link ApplicationContextInitializer}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@ContextConfiguration(initializers = CustomInitializer.class)
public class SpringBootTestContextBootstrapperWithInitializersTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void foundConfiguration() {
		Object bean = this.context
				.getBean(SpringBootTestContextBootstrapperExampleConfig.class);
		assertThat(bean).isNotNull();
	}

	// gh-8483

	public static class CustomInitializer
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
		}

	}

}
