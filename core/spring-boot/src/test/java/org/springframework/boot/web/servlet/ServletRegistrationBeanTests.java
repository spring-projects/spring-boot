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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.web.servlet.mock.MockServlet;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link ServletRegistrationBean}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ServletRegistrationBeanTests {

	private final MockServlet servlet = new MockServlet();

	@Mock
	@SuppressWarnings("NullAway.Init")
	private ServletContext servletContext;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private ServletRegistration.Dynamic registration;

	@Test
	void startupWithDefaults() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("mockServlet", this.servlet);
		then(this.registration).should().setAsyncSupported(true);
		then(this.registration).should().addMapping("/*");
	}

	@Test
	void failsWithDoubleRegistration() {
		assertThatIllegalStateException().isThrownBy(this::doubleRegistration)
			.withMessage("Failed to register 'servlet double-registration' on "
					+ "the servlet context. Possibly already registered?");
	}

	private void doubleRegistration() throws ServletException {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		bean.setName("double-registration");
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(null);
		bean.onStartup(this.servletContext);
	}

	@Test
	void startupWithSpecifiedValues() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
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
		then(this.servletContext).should().addServlet("test", this.servlet);
		then(this.registration).should().setAsyncSupported(false);
		Map<String, String> expectedInitParameters = new HashMap<>();
		expectedInitParameters.put("a", "b");
		expectedInitParameters.put("c", "d");
		then(this.registration).should().setInitParameters(expectedInitParameters);
		then(this.registration).should().addMapping("/a", "/b", "/c");
		then(this.registration).should().setLoadOnStartup(10);
	}

	@Test
	void specificName() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setName("specificName");
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("specificName", this.servlet);
	}

	@Test
	void deducedName() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(this.servlet);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should().addServlet("mockServlet", this.servlet);
	}

	@Test
	void disable() throws Exception {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		bean.setServlet(this.servlet);
		bean.setEnabled(false);
		bean.onStartup(this.servletContext);
		then(this.servletContext).should(never()).addServlet("mockServlet", this.servlet);
	}

	@Test
	void setServletMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>();
		assertThatIllegalStateException().isThrownBy(() -> bean.onStartup(this.servletContext))
			.withMessageContaining("Unable to return description for null servlet");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createServletMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServletRegistrationBean<MockServlet>(null))
			.withMessageContaining("'servlet' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void setMappingMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		assertThatIllegalArgumentException().isThrownBy(() -> bean.setUrlMappings(null))
			.withMessageContaining("'urlMappings' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createMappingMustNotBeNull() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ServletRegistrationBean<>(this.servlet, (String[]) null))
			.withMessageContaining("'urlMappings' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addMappingMustNotBeNull() {
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet);
		assertThatIllegalArgumentException().isThrownBy(() -> bean.addUrlMappings((String[]) null))
			.withMessageContaining("'urlMappings' must not be null");
	}

	@Test
	void setMappingReplacesValue() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, "/a", "/b");
		bean.setUrlMappings(new LinkedHashSet<>(Arrays.asList("/c", "/d")));
		bean.onStartup(this.servletContext);
		then(this.registration).should().addMapping("/c", "/d");
	}

	@Test
	void modifyInitParameters() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, "/a", "/b");
		bean.addInitParameter("a", "b");
		bean.getInitParameters().put("a", "c");
		bean.onStartup(this.servletContext);
		then(this.registration).should().setInitParameters(Collections.singletonMap("a", "c"));
	}

	@Test
	void withoutDefaultMappings() throws Exception {
		given(this.servletContext.addServlet(anyString(), any(Servlet.class))).willReturn(this.registration);
		ServletRegistrationBean<MockServlet> bean = new ServletRegistrationBean<>(this.servlet, false);
		bean.onStartup(this.servletContext);
		then(this.registration).should(never()).addMapping(any(String[].class));
	}

}
