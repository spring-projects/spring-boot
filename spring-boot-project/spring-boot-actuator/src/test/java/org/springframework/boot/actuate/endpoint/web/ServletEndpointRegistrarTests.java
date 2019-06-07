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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collections;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.actuate.endpoint.EndpointId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ServletEndpointRegistrar}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class ServletEndpointRegistrarTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private ServletContext servletContext;

	@Mock
	private Dynamic dynamic;

	@Captor
	private ArgumentCaptor<Servlet> servlet;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class))).willReturn(this.dynamic);
	}

	@Test
	public void createWhenServletEndpointsIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletEndpoints must not be null");
		new ServletEndpointRegistrar(null, null);
	}

	@Test
	public void onStartupShouldRegisterServlets() throws ServletException {
		assertBasePath(null, "/test/*");
	}

	@Test
	public void onStartupWhenHasBasePathShouldIncludeBasePath() throws ServletException {
		assertBasePath("/actuator", "/actuator/test/*");
	}

	@Test
	public void onStartupWhenHasEmptyBasePathShouldPrefixWithSlash() throws ServletException {
		assertBasePath("", "/test/*");
	}

	@Test
	public void onStartupWhenHasRootBasePathShouldNotAddDuplicateSlash() throws ServletException {
		assertBasePath("/", "/test/*");
	}

	private void assertBasePath(String basePath, String expectedMapping) throws ServletException {
		ExposableServletEndpoint endpoint = mockEndpoint(new EndpointServlet(TestServlet.class));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar(basePath, Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.servletContext).addServlet(eq("test-actuator-endpoint"), this.servlet.capture());
		assertThat(this.servlet.getValue()).isInstanceOf(TestServlet.class);
		verify(this.dynamic).addMapping(expectedMapping);
	}

	@Test
	public void onStartupWhenHasInitParametersShouldRegisterInitParameters() throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class).withInitParameter("a", "b"));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator", Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.dynamic).setInitParameters(Collections.singletonMap("a", "b"));
	}

	private ExposableServletEndpoint mockEndpoint(EndpointServlet endpointServlet) {
		ExposableServletEndpoint endpoint = mock(ExposableServletEndpoint.class);
		given(endpoint.getEndpointId()).willReturn(EndpointId.of("test"));
		given(endpoint.getEndpointServlet()).willReturn(endpointServlet);
		given(endpoint.getRootPath()).willReturn("test");
		return endpoint;
	}

	public static class TestServlet extends GenericServlet {

		@Override
		public void service(ServletRequest req, ServletResponse res) {
		}

	}

}
