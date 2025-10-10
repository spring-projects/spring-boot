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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesAutoConfigurationTests {

	private @Nullable AnnotationConfigApplicationContext context;

	@AfterEach
	void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void processAnnotatedBean() {
		load(new Class<?>[] { AutoConfig.class, SampleBean.class }, "foo.name:test");
		assertThat(getBean().getName()).isEqualTo("test");
	}

	@Test
	void processAnnotatedBeanNoAutoConfig() {
		load(new Class<?>[] { SampleBean.class }, "foo.name:test");
		assertThat(getBean().getName()).isEqualTo("default");
	}

	private void load(Class<?>[] configs, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(configs);
		TestPropertyValues.of(environment).applyTo(this.context);
		this.context.refresh();
	}

	private SampleBean getBean() {
		assertThat(this.context).isNotNull();
		return this.context.getBean(SampleBean.class);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(ConfigurationPropertiesAutoConfiguration.class)
	static class AutoConfig {

	}

	@Component
	@ConfigurationProperties("foo")
	static class SampleBean {

		private String name = "default";

		String getName() {
			return this.name;
		}

		void setName(String name) {
			this.name = name;
		}

	}

}
