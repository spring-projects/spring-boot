/*
 * Copyright 2012-present the original author or authors.
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
		assertThatIllegalStateException().isThrownBy(deducer::getPropertySources)
			.withMessage("Unable to obtain PropertySources from PropertySourcesPlaceholderConfigurer or Environment");
	}

	@Configuration(proxyBeanMethods = false)
	static class PropertySourcesPlaceholderConfigurerConfiguration {

		@Bean
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
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
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer1() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@Bean
		static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer2() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	private static class TestPropertySource extends MapPropertySource {

		TestPropertySource() {
			super("test", Collections.emptyMap());
		}

	}

}
