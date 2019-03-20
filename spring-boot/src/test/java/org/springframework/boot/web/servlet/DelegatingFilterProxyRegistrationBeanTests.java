/*
 * Copyright 2012-2016 the original author or authors.
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;

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
import static org.mockito.Matchers.isA;

/**
 * Tests for {@link DelegatingFilterProxyRegistrationBean}.
 *
 * @author Phillip Webb
 */
public class DelegatingFilterProxyRegistrationBeanTests
		extends AbstractFilterRegistrationBeanTests {

	private static ThreadLocal<Boolean> mockFilterInitialized = new ThreadLocal<Boolean>();

	private GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(
			new MockServletContext());

	@Test
	public void targetBeanNameMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TargetBeanName must not be null or empty");
		new DelegatingFilterProxyRegistrationBean(null);
	}

	@Test
	public void targetBeanNameMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TargetBeanName must not be null or empty");
		new DelegatingFilterProxyRegistrationBean("");
	}

	@Test
	public void nameDefaultsToTargetBeanName() throws Exception {
		assertThat(new DelegatingFilterProxyRegistrationBean("myFilter")
				.getOrDeduceName(null)).isEqualTo("myFilter");
	}

	@Test
	public void getFilterUsesDelegatingFilterProxy() throws Exception {
		AbstractFilterRegistrationBean registrationBean = createFilterRegistrationBean();
		Filter filter = registrationBean.getFilter();
		assertThat(filter).isInstanceOf(DelegatingFilterProxy.class);
		assertThat(ReflectionTestUtils.getField(filter, "webApplicationContext"))
				.isEqualTo(this.applicationContext);
		assertThat(ReflectionTestUtils.getField(filter, "targetBeanName"))
				.isEqualTo("mockFilter");
	}

	@Test
	public void initShouldNotCauseEarlyInitialization() throws Exception {
		this.applicationContext.registerBeanDefinition("mockFilter",
				new RootBeanDefinition(MockFilter.class));
		AbstractFilterRegistrationBean registrationBean = createFilterRegistrationBean();
		Filter filter = registrationBean.getFilter();
		filter.init(new MockFilterConfig());
		assertThat(mockFilterInitialized.get()).isNull();
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				new MockFilterChain());
		assertThat(mockFilterInitialized.get()).isEqualTo(true);
	}

	@Test
	public void createServletRegistrationBeanMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		new DelegatingFilterProxyRegistrationBean("mockFilter",
				(ServletRegistrationBean[]) null);
	}

	@Override
	protected AbstractFilterRegistrationBean createFilterRegistrationBean(
			ServletRegistrationBean... servletRegistrationBeans) {
		DelegatingFilterProxyRegistrationBean bean = new DelegatingFilterProxyRegistrationBean(
				"mockFilter", servletRegistrationBeans);
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
		public void doFilter(ServletRequest request, ServletResponse response,
				FilterChain chain) throws IOException, ServletException {
		}

	}

}
