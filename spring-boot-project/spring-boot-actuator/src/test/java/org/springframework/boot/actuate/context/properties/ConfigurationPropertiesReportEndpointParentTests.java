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

package org.springframework.boot.actuate.context.properties;

import org.junit.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ApplicationConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} when used with a parent
 * context.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class ConfigurationPropertiesReportEndpointParentTests {

	@Test
	public void configurationPropertiesClass() {
		new ApplicationContextRunner().withUserConfiguration(Parent.class).run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(ClassConfigurationProperties.class).withParent(parent)
					.run((child) -> {
						ConfigurationPropertiesReportEndpoint endpoint = child
								.getBean(ConfigurationPropertiesReportEndpoint.class);
						ApplicationConfigurationProperties applicationProperties = endpoint.configurationProperties();
						assertThat(applicationProperties.getContexts()).containsOnlyKeys(child.getId(), parent.getId());
						assertThat(applicationProperties.getContexts().get(child.getId()).getBeans().keySet())
								.containsExactly("someProperties");
						assertThat((applicationProperties.getContexts().get(parent.getId()).getBeans().keySet()))
								.containsExactly("testProperties");
					});
		});
	}

	@Test
	public void configurationPropertiesBeanMethod() {
		new ApplicationContextRunner().withUserConfiguration(Parent.class).run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(BeanMethodConfigurationProperties.class)
					.withParent(parent).run((child) -> {
						ConfigurationPropertiesReportEndpoint endpoint = child
								.getBean(ConfigurationPropertiesReportEndpoint.class);
						ApplicationConfigurationProperties applicationProperties = endpoint.configurationProperties();
						assertThat(applicationProperties.getContexts().get(child.getId()).getBeans().keySet())
								.containsExactlyInAnyOrder("otherProperties");
						assertThat((applicationProperties.getContexts().get(parent.getId()).getBeans().keySet()))
								.containsExactly("testProperties");
					});
		});
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Parent {

		@Bean
		public TestProperties testProperties() {
			return new TestProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class ClassConfigurationProperties {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		public TestProperties someProperties() {
			return new TestProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class BeanMethodConfigurationProperties {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		@ConfigurationProperties(prefix = "other")
		public OtherProperties otherProperties() {
			return new OtherProperties();
		}

	}

	public static class OtherProperties {

	}

	@ConfigurationProperties(prefix = "test")
	public static class TestProperties {

		private String myTestProperty = "654321";

		public String getMyTestProperty() {
			return this.myTestProperty;
		}

		public void setMyTestProperty(String myTestProperty) {
			this.myTestProperty = myTestProperty;
		}

	}

}
