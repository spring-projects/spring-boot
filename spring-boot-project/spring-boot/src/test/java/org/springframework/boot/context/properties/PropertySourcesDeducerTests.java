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

package org.springframework.boot.context.properties;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertySourcesDeducer}.
 *
 * @author Phillip Webb
 */
class PropertySourcesDeducerTests {

	@Test
	void getPropertySourcesWhenHasSinglePropertySourcesPlaceholderConfigurerReturnsBean() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				PropertySourcesPlaceholderConfigurerConfiguration.class);
		PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
		PropertySources propertySources = deducer.getPropertySources();
		assertThat(propertySources.get("test")).isInstanceOf(TestPropertySource.class);
	}

	@Test
	void getPropertySourcesWhenHasNoPropertySourcesPlaceholderConfigurerReturnsEnvironmentSources() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(EmptyConfiguration.class);
		ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
		environment.getPropertySources().addFirst(new TestPropertySource());
		PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
		PropertySources propertySources = deducer.getPropertySources();
		assertThat(propertySources.get("test")).isInstanceOf(TestPropertySource.class);
	}

	@Test
	void getPropertySourcesWhenHasMultiplePropertySourcesPlaceholderConfigurerReturnsEnvironmentSources() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				MultiplePropertySourcesPlaceholderConfigurerConfiguration.class);
		ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
		environment.getPropertySources().addFirst(new TestPropertySource());
		PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
		PropertySources propertySources = deducer.getPropertySources();
		assertThat(propertySources.get("test")).isInstanceOf(TestPropertySource.class);
	}

	@Test
	void getPropertySourcesWhenUnavailableThrowsException() {
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		Environment environment = mock(Environment.class);
		given(applicationContext.getEnvironment()).willReturn(environment);
		PropertySourcesDeducer deducer = new PropertySourcesDeducer(applicationContext);
		assertThatIllegalStateException().isThrownBy(() -> deducer.getPropertySources()).withMessage(
				"Unable to obtain PropertySources from PropertySourcesPlaceholderConfigurer or Environment");
	}

	@Configuration(proxyBeanMethods = false)
	static class PropertySourcesPlaceholderConfigurerConfiguration {

		@Bean
		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
			MutablePropertySources propertySources = new MutablePropertySources();
			propertySources.addFirst(new TestPropertySource());
			configurer.setPropertySources(propertySources);
			return configurer;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class MultiplePropertySourcesPlaceholderConfigurerConfiguration {

		@Bean
		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer1() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer2() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	private static class TestPropertySource extends MapPropertySource {

		TestPropertySource() {
			super("test", Collections.emptyMap());
		}

	}

}
