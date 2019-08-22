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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.web.servlet.mock.MockServlet;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServletRegistrationBean}.
 *
 * @author Phillip Webb
 */
class ServletRegistrationBeanTests {

	private final MockServlet servlet = new MockServlet();

	@Mock
	private ServletContext servletContext;

	@Mock
	private ServletRegistration.Dynamic registration;

	@Mock
	private FilterRegistration.Dynamic filterRegistration;

	@BeforeEach
	void setupMocks() {
		MockitoAnnotations.initMocks(this);
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		given(this.servletContext.addFilter(anyString(), any(Filter.class))).willReturn(this.filterRegistration);
	}

	@Test
	void startupWithDefaults() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("mockServlet", this.servlet);
		verify(this.registration).setAsyncSupported(true);
		verify(this.registration).addMapping("/*");
	}

	@Test
	void startupWithDoubleRegistration() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(null);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("mockServlet", this.servlet);
		verify(this.registration, never()).setAsyncSupported(true);
	}

	@Test
	void startupWithSpecifiedValues() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setName("test");
		bean.setServlet(this.servlet);
		bean.setAsyncSupported(false);
		bean.setInitParameters(Collections.singletonMap("a", "b"));
		bean.addInitParameter("c", "d");
		bean.setUrlMappings(new LinkedHashSet<>(Arrays.asList("/a", "/b")));
		bean.addUrlMappings("/c");
		bean.setLoadOnStartup(10);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("test", this.servlet);
		verify(this.registration).setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		verify(this.registration).setInitParameters(expectedInitParameters);
		verify(this.registration).addMapping("/a", "/b", "/c");
		verify(this.registration).setLoadOnStartup(10);
	}

	@Test
	void specificName() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setName("specificName");
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("specificName", this.servlet);
	}

	@Test
	void deducedName() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		verify(this.servletContext).addServlet("mockServlet", this.servlet);
	}

	@Test
	void disable() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(this.servlet);
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		verify(this.servletContext, never()).addServlet("mockServlet", this.servlet);
	}

	@Test
	void setServletMustNotBeNull() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		assertThatIllegalArgumentException().isThrownBy(() -> bean.onStartup(this.servletContext))
				.withMessageContaining("Servlet must not be null");
	}

	@Test
	void createServletMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServletRegistrationBean<MockServlet>(null))
				.withMessageContaining("Servlet must not be null");
	}

	@Test
	void setMappingMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setUrlMappings(null))
				.withMessageContaining("UrlMappings must not be null");
	}

	@Test
	void createMappingMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ServletRegistrationBean<>(this.servlet, (String[]) null))
				.withMessageContaining("UrlMappings must not be null");
	}

	@Test
	void addMappingMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addUrlMappings((String[]) null))
				.withMessageContaining("UrlMappings must not be null");
	}

	@Test
	void setMappingReplacesValue() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, "/a", "/b");
		bean.setUrlMappings(new LinkedHashSet<>(Arrays.asList("/c", "/d")));
		bean.onStartup(this.servletContext);
		verify(this.registration).addMapping("/c", "/d");
	}

	@Test
	void modifyInitParameters() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, "/a", "/b");
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		verify(this.registration).setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	void withoutDefaultMappings() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, false);
		bean.onStartup(this.servletContext);
		verify(this.registration, never()).addMapping(any(String[].class));
	}

}
