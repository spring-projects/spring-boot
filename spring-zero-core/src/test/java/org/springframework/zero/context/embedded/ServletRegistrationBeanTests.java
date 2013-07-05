/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.zero.context.embedded;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.zero.context.embedded.ServletRegistrationBean;

import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ServletRegistrationBean}.
 * 
 * @author Phillip Webb
 */
public class ServletRegistrationBeanTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MockServlet servlet = new MockServlet();

	@Mock
	private ServletContext servletContext;

	@Mock
	private ServletRegistration.Dynamic registration;

	@Mock
	private FilterRegistration.Dynamic filterRegistration;

	@Before
	public void setupMocks() {
		MockitoAnnotations.initMocks(this);
		given(this.servletContext.addServlet(anyString(), (Servlet) anyObject()))
				.willReturn(this.registration);
		given(this.servletContext.addFilter(anyString(), (Filter) anyObject()))
				.willReturn(this.filterRegistration);
	}

	@Test
	public void startupWithDefaults() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("mockServlet", this.servlet);
		verify(this.registration).setAsyncSupported(true);
		verify(this.registration).addMapping("/*");
	}

	@Test
	public void startupWithSpecifiedValues() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean();
		bean.setName("test");
		bean.setServlet(this.servlet);
		bean.setAsyncSupported(false);
		bean.setInitParameters(Collections.singletonMap("a", "b"));
		bean.addInitParameter("c", "d");
		bean.setUrlMappings(new LinkedHashSet<String>(Arrays.asList("/a", "/b")));
		bean.addUrlMappings("/c");
		bean.setLoadOnStartup(10);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("test", this.servlet);
		verify(this.registration).setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<String, String>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		verify(this.registration).setInitParameters(expectedInitParameters);
		verify(this.registration).addMapping("/a", "/b", "/c");
		verify(this.registration).setLoadOnStartup(10);
	}

	@Test
	public void specificName() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean();
		bean.setName("specificName");
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("specificName", this.servlet);
	}

	@Test
	public void deducedName() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean();
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("mockServlet", this.servlet);
	}

	@Test
	public void setServletMustNotBeNull() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean();
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Servlet must not be null");
		bean.onStartup(this.servletContext);
	}

	@Test
	public void createServletMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Servlet must not be null");
		new ServletRegistrationBean(null);
	}

	@Test
	public void setMappingMustNotBeNull() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet);
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("UrlMappings must not be null");
		bean.setUrlMappings(null);
	}

	@Test
	public void createMappingMustNotBeNull() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("UrlMappings must not be null");
		new ServletRegistrationBean(this.servlet, (String[]) null);
	}

	@Test
	public void addMappingMustNotBeNull() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet);
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("UrlMappings must not be null");
		bean.addUrlMappings((String[]) null);
	}

	@Test
	public void setMappingReplacesValue() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet, "/a",
				"/b");
		bean.setUrlMappings(new LinkedHashSet<String>(Arrays.asList("/c", "/d")));
		bean.onStartup(this.servletContext);
		verify(this.registration).addMapping("/c", "/d");
	}

	@Test
	public void modifyInitParameters() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet, "/a",
				"/b");
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		verify(this.registration).setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	public void filters() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet);
		Filter filter = new MockFilter();
		bean.addFilters(filter);
		bean.onStartup(this.servletContext);
		verify(servletContext).addFilter("mockFilter", filter);
		verify(filterRegistration).setAsyncSupported(true);
		verify(filterRegistration).addMappingForServletNames(
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD,
						DispatcherType.INCLUDE, DispatcherType.ASYNC), false,
				"mockServlet");
	}

	@Test
	public void filtersNoAsync() throws Exception {
		ServletRegistrationBean bean = new ServletRegistrationBean(this.servlet);
		Filter filter = new MockFilter();
		bean.addFilters(filter);
		bean.setAsyncSupported(false);
		bean.onStartup(this.servletContext);
		verify(servletContext).addFilter("mockFilter", filter);
		verify(filterRegistration).setAsyncSupported(false);
		verify(filterRegistration).addMappingForServletNames(
				EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD,
						DispatcherType.INCLUDE), false, "mockServlet");
	}
}
