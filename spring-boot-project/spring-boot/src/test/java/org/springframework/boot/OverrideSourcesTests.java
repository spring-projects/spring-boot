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

package org.springframework.boot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringApplication} {@link SpringApplication#setSources(java.util.Set)
 * source overrides}.
 *
 * @author Dave Syer
 */
class OverrideSourcesTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void beanInjectedToMainConfiguration() {
		this.context = SpringApplication.run(new Class<?>[] { MainConfiguration.class },
				new String[] { "--spring.main.web-application-type=none" });
		assertThat(this.context.getBean(Service.class).bean.name).isEqualTo("foo");
	}

	@Test
	void primaryBeanInjectedProvingSourcesNotOverridden() {
		this.context = SpringApplication.run(new Class<?>[] { MainConfiguration.class, TestConfiguration.class },
				new String[] { "--spring.main.web-application-type=none",
						"--spring.main.allow-bean-definition-overriding=true",
						"--spring.main.sources=org.springframework.boot.OverrideSourcesTests.MainConfiguration" });
		assertThat(this.context.getBean(Service.class).bean.name).isEqualTo("bar");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		@Primary
		TestBean another() {
			return new TestBean("bar");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MainConfiguration {

		@Bean
		TestBean first() {
			return new TestBean("foo");
		}

		@Bean
		Service Service() {
			return new Service();
		}

	}

	static class Service {

		@Autowired
		private TestBean bean;

	}

	static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

	}

}
