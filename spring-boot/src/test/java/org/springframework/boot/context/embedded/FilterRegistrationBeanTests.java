/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link FilterRegistrationBean}.
 *
 * @author Phillip Webb
 */
public class FilterRegistrationBeanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final MockFilter filter = new MockFilter();

	@Mock
	private ServletContext servletContext;

	@Mock
	private FilterRegistration.Dynamic registration;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
		given(this.servletContext.addFilter(anyString(), (Filter) anyObject()))
				.willReturn(this.registration);
	}

	@Test
	public void startupWithDefaults() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter("mockFilter", this.filter);
		verify(this.registration).setAsyncSupported(true);
		verify(this.registration).addMappingForUrlPatterns(
				FilterRegistrationBean.ASYNC_DISPATCHER_TYPES, false, "/*");
	}

	@Test
	public void startupWithSpecifiedValues() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setName("test");
		bean.setFilter(this.filter);
		bean.setAsyncSupported(false);
		bean.setInitParameters(Collections.singletonMap("a", "b"));
		bean.addInitParameter("c", "d");
		bean.setUrlPatterns(new LinkedHashSet<String>(Arrays.asList("/a", "/b")));
		bean.addUrlPatterns("/c");
		bean.setServletNames(new LinkedHashSet<String>(Arrays.asList("s1", "s2")));
		bean.addServletNames("s3");
		bean.setServletRegistrationBeans(Collections
				.singleton(mockServletRegistation("s4")));
		bean.addServletRegistrationBeans(mockServletRegistation("s5"));
		bean.setMatchAfter(true);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter("test", this.filter);
		verify(this.registration).setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<String, String>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		verify(this.registration).setInitParameters(expectedInitParameters);
		verify(this.registration)
				.addMappingForUrlPatterns(
						FilterRegistrationBean.NON_ASYNC_DISPATCHER_TYPES, true, "/a",
						"/b", "/c");
		verify(this.registration).addMappingForServletNames(
				FilterRegistrationBean.NON_ASYNC_DISPATCHER_TYPES, true, "s4", "s5",
				"s1", "s2", "s3");
	}

	@Test
	public void specificName() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setName("specificName");
		bean.setFilter(this.filter);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter("specificName", this.filter);
	}

	@Test
	public void deducedName() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(this.filter);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addFilter("mockFilter", this.filter);
	}

	@Test
	public void disable() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		bean.setFilter(this.filter);
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		verify(this.servletContext, times(0)).addFilter("mockFilter", this.filter);
	}

	@Test
	public void setFilterMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Filter must not be null");
		bean.onStartup(this.servletContext);
	}

	@Test
	public void createServletMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Filter must not be null");
		new FilterRegistrationBean(null);
	}

	@Test
	public void setServletRegistrationBeanMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		bean.setServletRegistrationBeans(null);
	}

	@Test
	public void createServletRegistrationBeanMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		new FilterRegistrationBean(this.filter, (ServletRegistrationBean[]) null);
	}

	@Test
	public void addServletRegistrationBeanMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletRegistrationBeans must not be null");
		bean.addServletRegistrationBeans((ServletRegistrationBean[]) null);
	}

	@Test
	public void setServletRegistrationBeanReplacesValue() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter,
				mockServletRegistation("a"));
		bean.setServletRegistrationBeans(new LinkedHashSet<ServletRegistrationBean>(
				Arrays.asList(mockServletRegistation("b"))));
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForServletNames(
				FilterRegistrationBean.ASYNC_DISPATCHER_TYPES, false, "b");
	}

	@Test
	public void modifyInitParameters() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		verify(this.registration).setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	public void setUrlPatternMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("UrlPatterns must not be null");
		bean.setUrlPatterns(null);
	}

	@Test
	public void addUrlPatternMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("UrlPatterns must not be null");
		bean.addUrlPatterns((String[]) null);
	}

	@Test
	public void setServletNameMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletNames must not be null");
		bean.setServletNames(null);
	}

	@Test
	public void addServletNameMustNotBeNull() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletNames must not be null");
		bean.addServletNames((String[]) null);
	}

	@Test
	public void withSpecificDispatcherTypes() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		bean.setDispatcherTypes(DispatcherType.INCLUDE, DispatcherType.FORWARD);
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForUrlPatterns(
				EnumSet.of(DispatcherType.INCLUDE, DispatcherType.FORWARD), false, "/*");
	}

	@Test
	public void withSpecificDispatcherTypesEnumSet() throws Exception {
		FilterRegistrationBean bean = new FilterRegistrationBean(this.filter);
		EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.INCLUDE,
				DispatcherType.FORWARD);
		bean.setDispatcherTypes(types);
		bean.onStartup(this.servletContext);
		verify(this.registration).addMappingForUrlPatterns(types, false, "/*");
	}

	private ServletRegistrationBean mockServletRegistation(String name) {
		ServletRegistrationBean bean = new ServletRegistrationBean();
		bean.setName(name);
		return bean;
	}
}
