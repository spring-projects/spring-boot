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

package org.springframework.boot.developertools.autoconfigure;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.developertools.remote.server.DispatcherFilter;
import org.springframework.boot.developertools.restart.MockRestarter;
import org.springframework.boot.developertools.restart.server.HttpRestartServer;
import org.springframework.boot.developertools.restart.server.SourceFolderUrlFilter;
import org.springframework.boot.test.EnvironmentTestUtils;
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteDeveloperToolsAutoConfiguration}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
public class RemoteDeveloperToolsAutoConfigurationTests {

	private static final String DEFAULT_CONTEXT_PATH = RemoteDeveloperToolsProperties.DEFAULT_CONTEXT_PATH;

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
	public void ignoresUnmappedUrl() throws Exception {
		loadContext("spring.developertools.remote.enabled:true");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/restart");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	public void invokeRestartWithDefaultSetup() throws Exception {
		loadContext("spring.developertools.remote.enabled:true");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(true);
	}

	@Test
	public void disableRestart() throws Exception {
		loadContext("spring.developertools.remote.enabled:true",
				"spring.developertools.remote.restart.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean("remoteRestartHanderMapper");
	}

	@Test
	public void developerToolsHealthReturns200() throws Exception {
		loadContext("spring.developertools.remote.enabled:true");
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH);
		this.response.setStatus(500);
		filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus(), equalTo(200));
	}

	private void assertRestartInvoked(boolean value) {
		assertThat(this.context.getBean(MockHttpRestartServer.class).invoked,
				equalTo(value));
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
	@Import(RemoteDeveloperToolsAutoConfiguration.class)
	static class Config {

		@Bean
		public HttpRestartServer remoteRestartHttpRestartServer() {
			SourceFolderUrlFilter sourceFolderUrlFilter = mock(SourceFolderUrlFilter.class);
			return new MockHttpRestartServer(sourceFolderUrlFilter);
		}

	}

	/**
	 * Mock {@link HttpRestartServer} implementation.
	 */
	static class MockHttpRestartServer extends HttpRestartServer {

		private boolean invoked;

		public MockHttpRestartServer(SourceFolderUrlFilter sourceFolderUrlFilter) {
			super(sourceFolderUrlFilter);
		}

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response)
				throws IOException {
			this.invoked = true;
		}

	}

}
