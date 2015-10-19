/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.env;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for the configuration of development-time properties
 *
 * @author Andy Wilkinson
 */
public class DevToolPropertiesIntegrationTests {

	private ConfigurableApplicationContext context;

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void classPropertyConditionIsAffectedByDevToolProperties() {
		SpringApplication application = new SpringApplication(
				ClassConditionConfiguration.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		this.context.getBean(ClassConditionConfiguration.class);
	}

	@Test
	public void beanMethodPropertyConditionIsAffectedByDevToolProperties() {
		SpringApplication application = new SpringApplication(
				BeanConditionConfiguration.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		this.context.getBean(MyBean.class);
	}

	@Configuration
	@ConditionalOnProperty("spring.h2.console.enabled")
	static class ClassConditionConfiguration {

	}

	@Configuration
	static class BeanConditionConfiguration {

		@Bean
		@ConditionalOnProperty("spring.h2.console.enabled")
		public MyBean myBean() {
			return new MyBean();
		}
	}

	static class MyBean {

	}

}
