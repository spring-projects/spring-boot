/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.context.properties;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ContextConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.endpoint.Show;
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
class ConfigurationPropertiesReportEndpointParentTests {

	@Test
	void configurationPropertiesClass() {
		new ApplicationContextRunner().withUserConfiguration(Parent.class).run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(ClassConfigurationProperties.class)
				.withParent(parent)
				.run((child) -> {
					ConfigurationPropertiesReportEndpoint endpoint = child
						.getBean(ConfigurationPropertiesReportEndpoint.class);
					ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
					assertThat(applicationProperties.getContexts()).containsOnlyKeys(child.getId(), parent.getId());
					ContextConfigurationPropertiesDescriptor childContext = applicationProperties.getContexts()
						.get(child.getId());
					assertThat(childContext).isNotNull();
					assertThat(childContext.getBeans().keySet()).containsExactly("someProperties");
					ContextConfigurationPropertiesDescriptor parentContext = applicationProperties.getContexts()
						.get(parent.getId());
					assertThat(parentContext).isNotNull();
					assertThat((parentContext.getBeans().keySet())).containsExactly("testProperties");
				});
		});
	}

	@Test
	void configurationPropertiesBeanMethod() {
		new ApplicationContextRunner().withUserConfiguration(Parent.class).run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(BeanMethodConfigurationProperties.class)
				.withParent(parent)
				.run((child) -> {
					ConfigurationPropertiesReportEndpoint endpoint = child
						.getBean(ConfigurationPropertiesReportEndpoint.class);
					ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
					ContextConfigurationPropertiesDescriptor childContext = applicationProperties.getContexts()
						.get(child.getId());
					assertThat(childContext).isNotNull();
					assertThat(childContext.getBeans().keySet()).containsExactlyInAnyOrder("otherProperties");
					ContextConfigurationPropertiesDescriptor parentContext = applicationProperties.getContexts()
						.get(parent.getId());
					assertThat(parentContext).isNotNull();
					assertThat((parentContext.getBeans().keySet())).containsExactly("testProperties");
				});
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class Parent {

		@Bean
		TestProperties testProperties() {
			return new TestProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class ClassConfigurationProperties {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

		@Bean
		TestProperties someProperties() {
			return new TestProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class BeanMethodConfigurationProperties {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

		@Bean
		@ConfigurationProperties("other")
		OtherProperties otherProperties() {
			return new OtherProperties();
		}

	}

	static class OtherProperties {

	}

	@ConfigurationProperties("test")
	static class TestProperties {

		private String myTestProperty = "654321";

		String getMyTestProperty() {
			return this.myTestProperty;
		}

		void setMyTestProperty(String myTestProperty) {
			this.myTestProperty = myTestProperty;
		}

	}

}
