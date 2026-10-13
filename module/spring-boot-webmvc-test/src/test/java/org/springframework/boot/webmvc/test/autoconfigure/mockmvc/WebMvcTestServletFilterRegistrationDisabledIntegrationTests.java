/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTest @WebMvcTest} with a disabled filter registration.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class WebMvcTestServletFilterRegistrationDisabledIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void shouldNotApplyFilter() {
		assertThat(this.mvc.get().uri("/one")).doesNotContainHeader("x-test");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class DisabledRegistrationConfiguration {

		@Bean
		FilterRegistrationBean<ExampleFilter> exampleFilterRegistration(ExampleFilter filter) {
			FilterRegistrationBean<ExampleFilter> registration = new FilterRegistrationBean<>(filter);
			registration.setEnabled(false);
			return registration;
		}

	}

}
