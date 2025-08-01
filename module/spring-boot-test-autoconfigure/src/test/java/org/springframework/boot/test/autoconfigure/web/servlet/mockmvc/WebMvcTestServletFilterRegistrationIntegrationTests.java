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

package org.springframework.boot.test.autoconfigure.web.servlet.mockmvc;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilterRegistration} and {@link FilterRegistrationBean} with
 * {@link WebMvcTest @WebMvcTest}.
 *
 * @author Dmytro Nosan
 */
@WebMvcTest
class WebMvcTestServletFilterRegistrationIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void annotation() {
		assertThat(this.mvc.get().uri("/annotation")).headers()
			.hasValue("name", "annotation")
			.hasValue("param1", "value1")
			.hasValue("param2", "value2")
			.doesNotContainHeader("param3")
			.doesNotContainHeader("param4");
	}

	@Test
	void registration() {
		assertThat(this.mvc.get().uri("/registration")).headers()
			.hasValue("name", "registration")
			.hasValue("param3", "value3")
			.hasValue("param4", "value4")
			.doesNotContainHeader("param1")
			.doesNotContainHeader("param2");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FilterRegistrationConfiguration {

		@Bean
		@FilterRegistration(name = "annotation", urlPatterns = "/annotation",
				initParameters = { @WebInitParam(name = "param1", value = "value1"),
						@WebInitParam(name = "param2", value = "value2") })
		@Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
		TestFilter testFilterAnnotationBean() {
			return new TestFilter();
		}

		@Bean
		FilterRegistrationBean<TestFilter> testFilterRegistrationBean() {
			FilterRegistrationBean<TestFilter> registration = new FilterRegistrationBean<>(new TestFilter());
			registration.setName("registration");
			registration.addUrlPatterns("/registration");
			registration.setInitParameters(Map.of("param3", "value3", "param4", "value4"));
			registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
			return registration;
		}

	}

	private static final class TestFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {
			response.addHeader("name", getFilterName());
			FilterConfig config = getFilterConfig();
			if (config != null) {
				Collections.list(config.getInitParameterNames())
					.forEach((name) -> response.addHeader(name, config.getInitParameter(name)));
			}
			filterChain.doFilter(request, response);
		}

	}

}
