/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.EnumSet;

import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcObservationManagementContextConfiguration}.
 *
 * @author Aditya Reddy
 */
class WebMvcObservationManagementContextConfigurationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebMvcObservationManagementContextConfiguration.class));

	@Test
	void backsOffWithoutObservationRegistry() {
		this.runner.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Test
	void registersFilterDespiteParentRegistration() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebMvcObservationAutoConfiguration.class))
			.withBean(ObservationRegistry.class, ObservationRegistry::create)
			.run((parent) -> this.runner.withParent(parent).run((context) -> {
				assertThat(context).hasBean("managementWebMvcObservationFilter");
				FilterRegistrationBean<?> registration = context.getBean("managementWebMvcObservationFilter",
						FilterRegistrationBean.class);
				assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
				assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes",
						EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
				assertThat(registration.getFilter()).extracting("observationConvention")
					.isSameAs(parent.getBean(DefaultServerRequestObservationConvention.class));
			}));
	}

	@Test
	void backsOffWithLocalFilter() {
		this.runner.withBean(ObservationRegistry.class, ObservationRegistry::create)
			.withBean(DefaultServerRequestObservationConvention.class)
			.withBean(ServerHttpObservationFilter.class,
					() -> new ServerHttpObservationFilter(ObservationRegistry.create()))
			.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Test
	void backsOffWithLocalFilterRegistration() {
		this.runner.withBean(ObservationRegistry.class, ObservationRegistry::create)
			.withBean(DefaultServerRequestObservationConvention.class)
			.withUserConfiguration(CustomFilterConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean("managementWebMvcObservationFilter")
				.hasSingleBean(FilterRegistrationBean.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFilterConfiguration {

		@Bean
		FilterRegistrationBean<ServerHttpObservationFilter> customObservationFilter() {
			return new FilterRegistrationBean<>(new ServerHttpObservationFilter(ObservationRegistry.create()));
		}

	}

}
