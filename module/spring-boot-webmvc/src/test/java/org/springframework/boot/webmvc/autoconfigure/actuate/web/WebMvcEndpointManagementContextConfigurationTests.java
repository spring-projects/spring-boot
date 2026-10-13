/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcEndpointManagementContextConfiguration}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@SuppressWarnings("removal")
class WebMvcEndpointManagementContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(TestConfig.class)
		.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
				EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class));

	@Test
	void contextShouldContainServletEndpointRegistrar() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ServletEndpointRegistrar.class);
			ServletEndpointRegistrar bean = context.getBean(ServletEndpointRegistrar.class);
			assertThat(bean).hasFieldOrPropertyWithValue("basePath", "/test/actuator");
		});
	}

	@Test
	void contextWhenNotServletBasedShouldNotContainServletEndpointRegistrar() {
		new ApplicationContextRunner().withUserConfiguration(TestConfig.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ServletEndpointRegistrar.class));
	}

	@Test
	@ClassPathExclusions(packages = "org.springframework.boot.health.actuate.endpoint")
	void refreshSucceedsWithoutHealth() {
		this.contextRunner.run((context) -> assertThat(context).hasNotFailed());
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(WebMvcEndpointManagementContextConfiguration.class)
	static class TestConfig {

		@Bean
		ServletEndpointsSupplier servletEndpointsSupplier() {
			return Collections::emptyList;
		}

		@Bean
		DispatcherServletPath dispatcherServletPath() {
			return () -> "/test";
		}

		@Bean
		EndpointAccessResolver endpointAccessResolver() {
			return (endpointId, defaultAccess) -> Access.UNRESTRICTED;
		}

	}

}
