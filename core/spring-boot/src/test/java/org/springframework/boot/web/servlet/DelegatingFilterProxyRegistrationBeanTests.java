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

package org.springframework.boot.web.servlet;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.filter.GenericFilterBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.isA;

/**
 * Tests for {@link DelegatingFilterProxyRegistrationBean}.
 *
 * @author Phillip Webb
 */
class DelegatingFilterProxyRegistrationBeanTests extends AbstractFilterRegistrationBeanTests {

	private static final ThreadLocal<Boolean> mockFilterInitialized = new ThreadLocal<>();

	private final GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(
			new MockServletContext());

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void targetBeanNameMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingFilterProxyRegistrationBean(null))
			.withMessageContaining("'targetBeanName' must not be empty");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void targetBeanNameMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingFilterProxyRegistrationBean(""))
			.withMessageContaining("'targetBeanName' must not be empty");
	}

	@Test
	void nameDefaultsToTargetBeanName() {
		assertThat(new DelegatingFilterProxyRegistrationBean("myFilter").getOrDeduceName(null)).isEqualTo("myFilter");
	}

	@Test
	void getFilterUsesDelegatingFilterProxy() {
		DelegatingFilterProxyRegistrationBean registrationBean = createFilterRegistrationBean();
		Filter filter = registrationBean.getFilter();
		assertThat(filter).isInstanceOf(DelegatingFilterProxy.class);
		assertThat(filter).extracting("webApplicationContext").isEqualTo(this.applicationContext);
		assertThat(filter).extracting("targetBeanName").isEqualTo("mockFilter");
	}

	@Test
	void initShouldNotCauseEarlyInitialization() throws Exception {
		this.applicationContext.registerBeanDefinition("mockFilter", new RootBeanDefinition(MockFilter.class));
		DelegatingFilterProxyRegistrationBean registrationBean = createFilterRegistrationBean();
		Filter filter = registrationBean.getFilter();
		filter.init(new MockFilterConfig());
		mockFilterInitialized.remove();
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
		assertThat(mockFilterInitialized.get()).isTrue();
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createServletRegistrationBeanMustNotBeNull() {
		assertThatIllegalArgumentException()
			.isThrownBy(
					() -> new DelegatingFilterProxyRegistrationBean("mockFilter", (ServletRegistrationBean<?>[]) null))
			.withMessageContaining("'servletRegistrationBeans' must not be null");
	}

	@Override
	protected DelegatingFilterProxyRegistrationBean createFilterRegistrationBean(
			ServletRegistrationBean<?>... servletRegistrationBeans) {
		DelegatingFilterProxyRegistrationBean bean = new DelegatingFilterProxyRegistrationBean("mockFilter",
				servletRegistrationBeans);
		bean.setApplicationContext(this.applicationContext);
		return bean;
	}

	@Override
	protected Filter getExpectedFilter() {
		return isA(DelegatingFilterProxy.class);
	}

	static class MockFilter extends GenericFilterBean {

		MockFilter() {
			mockFilterInitialized.set(true);
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		}

	}

}
