/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.io.IOException;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.servlet.mock.MockFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link FilterRegistrationBean}.
 *
 * @author Phillip Webb
 */
class FilterRegistrationBeanTests extends AbstractFilterRegistrationBeanTests {

	private final MockFilter filter = new MockFilter();

	private final OncePerRequestFilter oncePerRequestFilter = new OncePerRequestFilter() {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {
			filterChain.doFilter(request, response);
		}

	};

	@Test
	void setFilter() throws Exception {
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
		bean.setFilter(this.filter);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addFilter("mockFilter", this.filter);
	}

	@Test
	void setFilterMustNotBeNull() {
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.onStartup(this.servletContext))
				.withMessageContaining("Filter must not be null");
	}

	@Test
	void constructFilterMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FilterRegistrationBean<>(null))
				.withMessageContaining("Filter must not be null");
	}

	@Test
	void createServletRegistrationBeanMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FilterRegistrationBean<>(this.filter, (ServletRegistrationBean[]) null))
				.withMessageContaining("ServletRegistrationBeans must not be null");
	}

	@Test
	void startupWithOncePerRequestDefaults() throws Exception {
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.registration);
		FilterRegistrationBean<?> bean = new FilterRegistrationBean<>(this.oncePerRequestFilter);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addFilter(eq("oncePerRequestFilter"), eq(this.oncePerRequestFilter));
		then(this.registration).should().setAsyncSupported(true);
		then(this.registration).should().addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
	}

	@Override
	protected AbstractFilterRegistrationBean<MockFilter> createFilterRegistrationBean(
			ServletRegistrationBean<?>... servletRegistrationBeans) {
		return new FilterRegistrationBean<>(this.filter, servletRegistrationBeans);
	}

	@Override
	protected Filter getExpectedFilter() {
		return eq(this.filter);
	}

}
