/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import javax.servlet.Filter;

import org.junit.Test;

import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;

/**
 * Tests for {@link DelegatingFilterProxyRegistrationBean}.
 *
 * @author Phillip Webb
 */
public class DelegatingFilterProxyRegistrationBeanTests
		extends AbstractFilterRegistrationBeanTests {

	private WebApplicationContext applicationContext = new GenericWebApplicationContext(
			new MockServletContext());;

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
				.getOrDeduceName(null), equalTo("myFilter"));
	}

	@Test
	public void getFilterUsesDelegatingFilterProxy() throws Exception {
		AbstractFilterRegistrationBean registrationBean = createFilterRegistrationBean();
		Filter filter = registrationBean.getFilter();
		assertThat(filter, instanceOf(DelegatingFilterProxy.class));
		assertThat(ReflectionTestUtils.getField(filter, "webApplicationContext"),
				equalTo((Object) this.applicationContext));
		assertThat(ReflectionTestUtils.getField(filter, "targetBeanName"),
				equalTo((Object) "mockFilter"));
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

}
