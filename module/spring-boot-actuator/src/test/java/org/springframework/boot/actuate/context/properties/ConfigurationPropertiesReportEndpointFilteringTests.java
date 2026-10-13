/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.context.properties;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ContextConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} when filtering by prefix.
 *
 * @author Chris Bono
 */
class ConfigurationPropertiesReportEndpointFilteringTests {

	@Test
	void filterByPrefixSingleMatch() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(Config.class)
			.withPropertyValues("foo.primary.name:foo1", "foo.secondary.name:foo2", "only.bar.name:solo1");
		assertProperties(contextRunner, "solo1");
	}

	@Test
	void filterByPrefixMultipleMatches() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(Config.class)
			.withPropertyValues("foo.primary.name:foo1", "foo.secondary.name:foo2", "only.bar.name:solo1");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint
				.configurationPropertiesWithPrefix("foo.");
			assertThat(applicationProperties.getContexts()).containsOnlyKeys(context.getId());
			ContextConfigurationPropertiesDescriptor contextProperties = applicationProperties.getContexts()
				.get(context.getId());
			assertThat(contextProperties).isNotNull();
			assertThat(contextProperties.getBeans()).containsOnlyKeys("primaryFoo", "secondaryFoo");
		});
	}

	@Test
	void filterByPrefixNoMatches() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(Config.class)
			.withPropertyValues("foo.primary.name:foo1", "foo.secondary.name:foo2", "only.bar.name:solo1");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint
				.configurationPropertiesWithPrefix("foo.third");
			assertThat(applicationProperties.getContexts()).containsOnlyKeys(context.getId());
			ContextConfigurationPropertiesDescriptor contextProperties = applicationProperties.getContexts()
				.get(context.getId());
			assertThat(contextProperties).isNotNull();
			assertThat(contextProperties.getBeans()).isEmpty();
		});
	}

	@Test
	void noSanitizationWhenShowAlways() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ConfigWithAlways.class)
			.withPropertyValues("foo.primary.name:foo1", "foo.secondary.name:foo2", "only.bar.name:solo1");
		assertProperties(contextRunner, "solo1");
	}

	@Test
	void sanitizationWhenShowNever() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(ConfigWithNever.class)
			.withPropertyValues("foo.primary.name:foo1", "foo.secondary.name:foo2", "only.bar.name:solo1");
		assertProperties(contextRunner, "******");
	}

	private void assertProperties(ApplicationContextRunner contextRunner, String value) {
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint
				.configurationPropertiesWithPrefix("only.bar");
			assertThat(applicationProperties.getContexts()).containsOnlyKeys(context.getId());
			ContextConfigurationPropertiesDescriptor contextProperties = applicationProperties.getContexts()
				.get(context.getId());
			assertThat(contextProperties).isNotNull();
			Optional<String> key = contextProperties.getBeans()
				.keySet()
				.stream()
				.filter((id) -> findIdFromPrefix("only.bar", id))
				.findAny();
			ConfigurationPropertiesBeanDescriptor descriptor = contextProperties.getBeans().get(key.get());
			assertThat(descriptor).isNotNull();
			assertThat(descriptor.getPrefix()).isEqualTo("only.bar");
			assertThat(descriptor.getProperties()).containsEntry("name", value);
		});
	}

	private boolean findIdFromPrefix(String prefix, String id) {
		int separator = id.indexOf("-");
		String candidate = (separator != -1) ? id.substring(0, separator) : id;
		return prefix.equals(candidate);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	@EnableConfigurationProperties(Bar.class)
	static class Config {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.WHEN_AUTHORIZED);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	@EnableConfigurationProperties(Bar.class)
	static class ConfigWithNever {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.NEVER);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	@EnableConfigurationProperties(Bar.class)
	static class ConfigWithAlways {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(Bar.class)
	static class BaseConfiguration {

		@Bean
		@ConfigurationProperties("foo.primary")
		Foo primaryFoo() {
			return new Foo();
		}

		@Bean
		@ConfigurationProperties("foo.secondary")
		Foo secondaryFoo() {
			return new Foo();
		}

	}

	public static class Foo {

		private String name = "5150";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@ConfigurationProperties("only.bar")
	public static class Bar {

		private String name = "123456";

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
