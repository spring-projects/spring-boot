/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.server.HttpRestartServer;
import org.springframework.boot.devtools.restart.server.SourceFolderUrlFilter;
import org.springframework.boot.test.util.TestPropertyValues;
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
	public void disabledIfRemoteSecretIsMissing() {
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
				"server.servlet.context-path:/test");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(true);
	}

	@Test
	public void disableRestart() {
		loadContext("spring.devtools.remote.secret:supersecret",
				"spring.devtools.remote.restart.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean("remoteRestartHandlerMapper");
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
				"server.servlet.context-path:/test");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH);
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		this.response.setStatus(500);
		filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	private void assertRestartInvoked(boolean value) {
		assertThat(this.context.getBean(MockHttpRestartServer.class).invoked)
				.isEqualTo(value);
	}

	private void loadContext(String... properties) {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(Config.class, PropertyPlaceholderAutoConfiguration.class);
		TestPropertyValues.of(properties).applyTo(this.context);
		this.context.refresh();
	}

	@Configuration
	@Import(RemoteDevToolsAutoConfiguration.class)
	static class Config {

		@Bean
		public HttpRestartServer remoteRestartHttpRestartServer() {
			SourceFolderUrlFilter sourceFolderUrlFilter = mock(
					SourceFolderUrlFilter.class);
			return new MockHttpRestartServer(sourceFolderUrlFilter);
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
		public void handle(ServerHttpRequest request, ServerHttpResponse response) {
			this.invoked = true;
		}

	}

}
