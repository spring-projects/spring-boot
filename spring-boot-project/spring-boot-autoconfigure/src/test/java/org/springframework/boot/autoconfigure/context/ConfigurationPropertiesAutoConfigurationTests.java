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

package org.springframework.boot.autoconfigure.context;

import org.junit.After;
import org.junit.Test;

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
public class ConfigurationPropertiesAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void processAnnotatedBean() {
		load(new Class[] { AutoConfig.class, SampleBean.class }, "foo.name:test");
		assertThat(this.context.getBean(SampleBean.class).getName()).isEqualTo("test");
	}

	@Test
	public void processAnnotatedBeanNoAutoConfig() {
		load(new Class[] { SampleBean.class }, "foo.name:test");
		assertThat(this.context.getBean(SampleBean.class).getName()).isEqualTo("default");
	}

	private void load(Class<?>[] configs, String... environment) {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(configs);
		TestPropertyValues.of(environment).applyTo(this.context);
		this.context.refresh();
	}

	@Configuration
	@ImportAutoConfiguration(ConfigurationPropertiesAutoConfiguration.class)
	static class AutoConfig {

	}

	@Component
	@ConfigurationProperties("foo")
	static class SampleBean {

		private String name = "default";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
