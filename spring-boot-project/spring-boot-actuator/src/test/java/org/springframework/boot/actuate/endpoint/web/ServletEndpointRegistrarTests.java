/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
 * @author Madhura Bhave
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
		given(this.servletContext.addServlet(any(String.class), any(Servlet.class)))
				.willReturn(this.dynamic);
	}

	@Test
	public void createWhenServletEndpointsIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ServletEndpoints must not be null");
		new ServletEndpointRegistrar((String) null, null);
	}

	@Test
	public void onStartupShouldRegisterServlets() throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar((String) null,
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.servletContext).addServlet(eq("test-actuator-endpoint"),
				this.servlet.capture());
		assertThat(this.servlet.getValue()).isInstanceOf(TestServlet.class);
		verify(this.dynamic).addMapping("/test/*");
	}

	@Test
	public void onStartupWhenHasBasePathShouldIncludeBasePath() throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator",
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.servletContext).addServlet(eq("test-actuator-endpoint"),
				this.servlet.capture());
		assertThat(this.servlet.getValue()).isInstanceOf(TestServlet.class);
		verify(this.dynamic).addMapping("/actuator/test/*");
	}

	@Test
	public void onStartupWhenHasMultipleBasePathsShouldIncludeAllBasePaths()
			throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class));
		Set<String> basePaths = new LinkedHashSet<>();
		basePaths.add("/actuator");
		basePaths.add("/admin");
		basePaths.add("/application");
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar(basePaths,
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.servletContext).addServlet(eq("test-actuator-endpoint"),
				this.servlet.capture());
		assertThat(this.servlet.getValue()).isInstanceOf(TestServlet.class);
		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		verify(this.dynamic).addMapping(captor.capture());
		assertThat(captor.getAllValues()).containsExactlyInAnyOrder("/application/test/*",
				"/admin/test/*", "/actuator/test/*");
	}

	@Test
	public void onStartupWhenHasEmptyBasePathsShouldIncludeRoot() throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class));
		Set<String> basePaths = Collections.emptySet();
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar(basePaths,
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.dynamic).addMapping("/test/*");
	}

	@Test
	public void onStartupWhenHasBasePathsHasNullValueShouldIncludeRoot()
			throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class));
		Set<String> basePaths = new LinkedHashSet<>();
		basePaths.add(null);
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar(basePaths,
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.dynamic).addMapping("/test/*");
	}

	@Test
	public void onStartupWhenDuplicateValuesShouldIncludeDistinct() throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class));
		Set<String> basePaths = new LinkedHashSet<>();
		basePaths.add("");
		basePaths.add(null);
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar(basePaths,
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.dynamic).addMapping("/test/*");
	}

	@Test
	public void onStartupWhenHasInitParametersShouldRegisterInitParameters()
			throws Exception {
		ExposableServletEndpoint endpoint = mockEndpoint(
				new EndpointServlet(TestServlet.class).withInitParameter("a", "b"));
		ServletEndpointRegistrar registrar = new ServletEndpointRegistrar("/actuator",
				Collections.singleton(endpoint));
		registrar.onStartup(this.servletContext);
		verify(this.dynamic).setInitParameters(Collections.singletonMap("a", "b"));
	}

	private ExposableServletEndpoint mockEndpoint(EndpointServlet endpointServlet) {
		ExposableServletEndpoint endpoint = mock(ExposableServletEndpoint.class);
		given(endpoint.getId()).willReturn("test");
		given(endpoint.getEndpointServlet()).willReturn(endpointServlet);
		given(endpoint.getRootPath()).willReturn("test");
		return endpoint;
	}

	public static class TestServlet extends GenericServlet {

		@Override
		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
		}

	}

}
