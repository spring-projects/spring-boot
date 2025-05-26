/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collections;
import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServletEndpointRegistrar}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "deprecation", "removal" })
class ServletEndpointRegistrarTests {

	@Mock
	private ServletContext servletContext;

	@Mock
	private ServletRegistration.Dynamic servletDynamic;

	@Mock
	private FilterRegistration.Dynamic filterDynamic;

	@Test
	void createWhenServletEndpointsIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ServletEndpointRegistrar(null, null))
			.withMessageContaining("'servletEndpoints' must not be null");
	}

	@Test
	void onStartupShouldRegisterServlets() throws ServletException {
		assertBasePath(null, "/test/*");
	}

	@Test
	void onStartupWhenHasBasePathShouldIncludeBasePath() throws ServletException {
		assertBasePath("/actuator", "/actuator/test/*");
	}

	@Test
	void onStartupWhenHasEmptyBasePathShouldPrefixWithSlash() throws ServletException {
		assertBasePath("", "/test/*");
	}

	@Test
	void onStartupWhenHasRootBasePathShouldNotAddDuplicateSlash() throws ServletException {
		assertBasePath("/", "/test/*");
	}

	private void assertBasePath(String basePath, String expectedMapping) throws ServletException {
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class))).willReturn(this.servletDynamic);
		ExposableServletEndpoint endpoint = mockEndpoint(new EndpointServlet(TestServlet.class));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar(basePath, Collections.singleton(endpoint),
				(endpointId, defaultAccess) -> Access.UNRESTRICTED);
		registrar.onStartup(this.servletContext);
		then(this.servletContext).should()
			.addServlet(eq("test-actuator-endpoint"),
					(Servlet) assertArg((servlet) -> assertThat(servlet).isInstanceOf(TestServlet.class)));
		then(this.servletDynamic).should().addMapping(expectedMapping);
		then(this.servletContext).shouldHaveNoMoreInteractions();
	}

	@Test
	void onStartupWhenHasInitParametersShouldRegisterInitParameters() throws Exception {
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class))).willReturn(this.servletDynamic);
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class).withInitParameter("a", "b"));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator", Collections.singleton(endpoint),
				(endpointId, defaultAccess) -> Access.UNRESTRICTED);
		registrar.onStartup(this.servletContext);
		then(this.servletDynamic).should().setInitParameters(Collections.singletonMap("a", "b"));
	}

	@Test
	void onStartupWhenHasLoadOnStartupShouldRegisterLoadOnStartup() throws Exception {
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class))).willReturn(this.servletDynamic);
		ExposableServletEndpoint endpoint = mockEndpoint(new EndpointServlet(TestServlet.class).withLoadOnStartup(7));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator", Collections.singleton(endpoint),
				(endpointId, defaultAccess) -> Access.UNRESTRICTED);
		registrar.onStartup(this.servletContext);
		then(this.servletDynamic).should().setLoadOnStartup(7);
	}

	@Test
	void onStartupWhenHasNotLoadOnStartupShouldRegisterDefaultValue() throws Exception {
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class))).willReturn(this.servletDynamic);
		ExposableServletEndpoint endpoint = mockEndpoint(new EndpointServlet(TestServlet.class));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator", Collections.singleton(endpoint),
				(endpointId, defaultAccess) -> Access.UNRESTRICTED);
		registrar.onStartup(this.servletContext);
		then(this.servletDynamic).should().setLoadOnStartup(-1);
	}

	@Test
	void onStartupWhenAccessIsDisabledShouldNotRegister() throws Exception {
		ExposableServletEndpoint endpoint = mock(ExposableServletEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(EndpointId.of("test"));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator", Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		then(this.servletContext).shouldHaveNoInteractions();
	}

	@Test
	void onStartupWhenAccessIsReadOnlyShouldRegisterServletWithFilter() throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(new EndpointServlet(TestServlet.class));
		given(endpoint.getEndpointId()).willReturn(EndpointId.of("test"));
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class))).willReturn(this.servletDynamic);
		given(this.servletContext.addFilter(any(String.class), any(Filter.class))).willReturn(this.filterDynamic);
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator", Collections.singleton(endpoint),
				(endpointId, defaultAccess) -> Access.READ_ONLY);
		registrar.onStartup(this.servletContext);
		then(this.servletContext).should()
			.addServlet(eq("test-actuator-endpoint"),
					(Servlet) assertArg((servlet) -> assertThat(servlet).isInstanceOf(TestServlet.class)));
		then(this.servletDynamic).should().addMapping("/actuator/test/*");
		then(this.servletContext).should()
			.addFilter(eq("test-actuator-endpoint-access-filter"), (Filter) assertArg((filter) -> assertThat(filter)
				.isInstanceOf(
						org.springframework.boot.actuate.endpoint.web.ServletEndpointRegistrar.ReadOnlyAccessFilter.class)));
		then(this.filterDynamic).should()
			.addMappingForServletNames(EnumSet.allOf(DispatcherType.class), false, "test-actuator-endpoint");
	}

	private ExposableServletEndpoint mockEndpoint(EndpointServlet endpointServlet) {
		ExposableServletEndpoint endpoint = mock(ExposableServletEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(EndpointId.of("test"));
		given(endpoint.getEndpointServlet()).willReturn(endpointServlet);
		given(endpoint.getRootPath()).willReturn("test");
		return endpoint;
	}

	static class TestServlet extends GenericServlet {

		@Override
		public void service(ServletRequest req, ServletResponse res) {
		}

	}

}
