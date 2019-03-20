/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.server.HttpRestartServer;
import org.springframework.boot.devtools.restart.server.SourceFolderUrlFilter;
import org.springframework.boot.devtools.tunnel.server.HttpTunnelServer;
import org.springframework.boot.devtools.tunnel.server.RemoteDebugPortProvider;
import org.springframework.boot.devtools.tunnel.server.SocketTargetServerConnection;
import org.springframework.boot.devtools.tunnel.server.TargetServerConnection;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteDevToolsAutoConfiguration}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
public class RemoteDevToolsAutoConfigurationTests {

	private static final String DEFAULT_CONTEXT_PATH = RemoteDevToolsProperties.DEFAULT_CONTEXT_PATH;

	private static final String DEFAULT_SECRET_HEADER_NAME = RemoteDevToolsProperties.DEFAULT_SECRET_HEADER_NAME;

	@Rule
	public MockRestarter mockRestarter = new MockRestarter();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigWebApplicationContext context;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain chain;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.chain = new MockFilterChain();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void disabledIfRemoteSecretIsMissing() throws Exception {
		loadContext("a:b");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(DispatcherFilter.class);
	}

	@Test
	public void ignoresUnmappedUrl() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	public void ignoresIfMissingSecretFromRequest() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	public void ignoresInvalidSecretInRequest() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "invalid");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	public void invokeRestartWithDefaultSetup() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(true);
	}

	@Test
	public void invokeRestartWithCustomServerContextPath() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret",
				"server.context-path:/test");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(true);
	}

	@Test
	public void disableRestart() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret",
				"spring.devtools.remote.restart.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean("remoteRestartHandlerMapper");
	}

	@Test
	public void invokeTunnelWithDefaultSetup() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/debug");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertTunnelInvoked(true);
	}

	@Test
	public void invokeTunnelWithCustomServerContextPath() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret",
				"server.context-path:/test");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH + "/debug");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertTunnelInvoked(true);
	}

	@Test
	public void invokeTunnelWithCustomHeaderName() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret",
				"spring.devtools.remote.secretHeaderName:customheader");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/debug");
		this.request.addHeader("customheader", "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertTunnelInvoked(true);
	}

	@Test
	public void disableRemoteDebug() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret",
				"spring.devtools.remote.debug.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean("remoteDebugHandlerMapper");
	}

	@Test
	public void devToolsHealthReturns200() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH);
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		this.response.setStatus(500);
		filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	@Test
	public void devToolsHealthWithCustomServerContextPathReturns200() throws Exception {
		loadContext("spring.devtools.remote.secret:supersecret",
				"server.context-path:/test");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH);
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		this.response.setStatus(500);
		filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	private void assertTunnelInvoked(boolean value) {
		assertThat(this.context.getBean(MockHttpTunnelServer.class).invoked)
				.isEqualTo(value);
	}

	private void assertRestartInvoked(boolean value) {
		assertThat(this.context.getBean(MockHttpRestartServer.class).invoked)
				.isEqualTo(value);
	}

	private void loadContext(String... properties) {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, properties);
		this.context.refresh();
	}

	@Configuration
	@Import(RemoteDevToolsAutoConfiguration.class)
	static class Config {

		@Bean
		public HttpTunnelServer remoteDebugHttpTunnelServer() {
			return new MockHttpTunnelServer(
					new SocketTargetServerConnection(new RemoteDebugPortProvider()));
		}

		@Bean
		public HttpRestartServer remoteRestartHttpRestartServer() {
			SourceFolderUrlFilter sourceFolderUrlFilter = mock(
					SourceFolderUrlFilter.class);
			return new MockHttpRestartServer(sourceFolderUrlFilter);
		}

	}

	/**
	 * Mock {@link HttpTunnelServer} implementation.
	 */
	static class MockHttpTunnelServer extends HttpTunnelServer {

		private boolean invoked;

		MockHttpTunnelServer(TargetServerConnection serverConnection) {
			super(serverConnection);
		}

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response)
				throws IOException {
			this.invoked = true;
		}

	}

	/**
	 * Mock {@link HttpRestartServer} implementation.
	 */
	static class MockHttpRestartServer extends HttpRestartServer {

		private boolean invoked;

		MockHttpRestartServer(SourceFolderUrlFilter sourceFolderUrlFilter) {
			super(sourceFolderUrlFilter);
		}

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response)
				throws IOException {
			this.invoked = true;
		}

	}

}
