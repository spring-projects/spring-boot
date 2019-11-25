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

package org.springframework.boot.devtools.autoconfigure;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.servlet.Filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.server.HttpRestartServer;
import org.springframework.boot.devtools.restart.server.SourceFolderUrlFilter;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.BeanIds;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link RemoteDevToolsAutoConfiguration}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockRestarter.class)
class RemoteDevToolsAutoConfigurationTests {

	private static final String DEFAULT_CONTEXT_PATH = RemoteDevToolsProperties.DEFAULT_CONTEXT_PATH;

	private static final String DEFAULT_SECRET_HEADER_NAME = RemoteDevToolsProperties.DEFAULT_SECRET_HEADER_NAME;

	private AnnotationConfigServletWebApplicationContext context;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain chain;

	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.chain = new MockFilterChain();
	}

	@AfterEach
	void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void disabledIfRemoteSecretIsMissing() throws Exception {
		this.context = getContext(() -> loadContext("a:b"));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(DispatcherFilter.class));
	}

	@Test
	void ignoresUnmappedUrl() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	void ignoresIfMissingSecretFromRequest() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	void ignoresInvalidSecretInRequest() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "invalid");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(false);
	}

	@Test
	void invokeRestartWithDefaultSetup() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(true);
	}

	@Test
	void invokeRestartWithCustomServerContextPath() throws Exception {
		this.context = getContext(
				() -> loadContext("spring.devtools.remote.secret:supersecret", "server.servlet.context-path:/test"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH + "/restart");
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		filter.doFilter(this.request, this.response, this.chain);
		assertRestartInvoked(true);
	}

	@Test
	void securityConfigurationShouldAllowAccess() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).apply(springSecurity()).addFilter(filter)
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get(DEFAULT_CONTEXT_PATH + "/restart").header(DEFAULT_SECRET_HEADER_NAME,
				"supersecret")).andExpect(status().isOk());
		assertRestartInvoked(true);
	}

	@Test
	void securityConfigurationShouldAllowAccessToCustomPath() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret",
				"server.servlet.context-path:/test", "spring.devtools.remote.context-path:/custom"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).apply(springSecurity()).addFilter(filter)
				.build();
		mockMvc.perform(
				MockMvcRequestBuilders.get("/test/custom/restart").header(DEFAULT_SECRET_HEADER_NAME, "supersecret"))
				.andExpect(status().isOk());
		assertRestartInvoked(true);
	}

	@Test
	void securityConfigurationDoesNotAffectOtherPaths() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		Filter securityFilterChain = this.context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).addFilter(securityFilterChain)
				.addFilter(filter).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/my-path")).andExpect(status().isUnauthorized());
	}

	@Test
	void disableRestart() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret",
				"spring.devtools.remote.restart.enabled:false"));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean("remoteRestartHandlerMapper"));
	}

	@Test
	void devToolsHealthReturns200() throws Exception {
		this.context = getContext(() -> loadContext("spring.devtools.remote.secret:supersecret"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI(DEFAULT_CONTEXT_PATH);
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		this.response.setStatus(500);
		filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	@Test
	void devToolsHealthWithCustomServerContextPathReturns200() throws Exception {
		this.context = getContext(
				() -> loadContext("spring.devtools.remote.secret:supersecret", "server.servlet.context-path:/test"));
		DispatcherFilter filter = this.context.getBean(DispatcherFilter.class);
		this.request.setRequestURI("/test" + DEFAULT_CONTEXT_PATH);
		this.request.addHeader(DEFAULT_SECRET_HEADER_NAME, "supersecret");
		this.response.setStatus(500);
		filter.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	private AnnotationConfigServletWebApplicationContext getContext(
			Supplier<AnnotationConfigServletWebApplicationContext> supplier) throws Exception {
		AtomicReference<AnnotationConfigServletWebApplicationContext> atomicReference = new AtomicReference<>();
		Thread thread = new Thread(() -> {
			AnnotationConfigServletWebApplicationContext context = supplier.get();
			atomicReference.getAndSet(context);
		});
		thread.start();
		thread.join();
		return atomicReference.get();
	}

	private void assertRestartInvoked(boolean value) {
		assertThat(this.context.getBean(MockHttpRestartServer.class).invoked).isEqualTo(value);
	}

	private AnnotationConfigServletWebApplicationContext loadContext(String... properties) {
		AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(Config.class, PropertyPlaceholderAutoConfiguration.class);
		TestPropertyValues.of(properties).applyTo(context);
		context.refresh();
		return context;
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ SecurityAutoConfiguration.class, RemoteDevToolsAutoConfiguration.class })
	static class Config {

		@Bean
		HttpRestartServer remoteRestartHttpRestartServer() {
			SourceFolderUrlFilter sourceFolderUrlFilter = mock(SourceFolderUrlFilter.class);
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
