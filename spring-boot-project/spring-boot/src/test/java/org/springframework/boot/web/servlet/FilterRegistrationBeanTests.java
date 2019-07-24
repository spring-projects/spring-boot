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

import org.junit.Test;

import org.springframework.boot.web.servlet.mock.MockFilter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FilterRegistrationBean}.
 *
 * @author Phillip Webb
 */
public class FilterRegistrationBeanTests extends AbstractFilterRegistrationBeanTests {

	private final MockFilter filter = new MockFilter();

	@Test
	public void setFilter() throws Exception {
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
		bean.setFilter(this.filter);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter("mockFilter", this.filter);
	}

	@Test
	public void setFilterMustNotBeNull() throws Exception {
		FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Filter must not be null");
		bean.onStartup(this.servletContext);
	}

	@Test
	public void constructFilterMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Filter must not be null");
		new FilterRegistrationBean<>(null);
	}

	@Test
	public void createServletRegistrationBeanMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		new FilterRegistrationBean<>(this.filter, (ServletRegistrationBean[]) null);
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
