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

package org.springframework.boot.web.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
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

	private static ThreadLocal<Boolean> mockFilterInitialized = new ThreadLocal<>();

	private GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(
			new MockServletContext());

	@Test
	void targetBeanNameMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingFilterProxyRegistrationBean(null))
				.withMessageContaining("TargetBeanName must not be null or empty");
	}

	@Test
	void targetBeanNameMustNotBeEmpty() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingFilterProxyRegistrationBean(""))
				.withMessageContaining("TargetBeanName must not be null or empty");
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
		assertThat(ReflectionTestUtils.getField(filter, "webApplicationContext")).isEqualTo(this.applicationContext);
		assertThat(ReflectionTestUtils.getField(filter, "targetBeanName")).isEqualTo("mockFilter");
	}

	@Test
	void initShouldNotCauseEarlyInitialization() throws Exception {
		this.applicationContext.registerBeanDefinition("mockFilter", new RootBeanDefinition(MockFilter.class));
		DelegatingFilterProxyRegistrationBean registrationBean = createFilterRegistrationBean();
		Filter filter = registrationBean.getFilter();
		filter.init(new MockFilterConfig());
		assertThat(mockFilterInitialized.get()).isNull();
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());
		assertThat(mockFilterInitialized.get()).isTrue();
	}

	@Test
	void createServletRegistrationBeanMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(
						() -> new DelegatingFilterProxyRegistrationBean("mockFilter", (ServletRegistrationBean[]) null))
				.withMessageContaining("ServletRegistrationBeans must not be null");
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
