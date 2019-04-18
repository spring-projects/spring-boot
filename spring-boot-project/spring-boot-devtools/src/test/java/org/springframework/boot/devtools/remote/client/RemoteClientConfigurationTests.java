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

package org.springframework.boot.devtools.remote.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.remote.client.RemoteClientConfiguration.LiveReloadConfiguration;
import org.springframework.boot.devtools.remote.server.Dispatcher;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.RestartScopeInitializer;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RemoteClientConfiguration}.
 *
 * @author Phillip Webb
 */
public class RemoteClientConfigurationTests {

	@Rule
	public MockRestarter restarter = new MockRestarter();

	@Rule
	public OutputCapture output = new OutputCapture();

	private AnnotationConfigServletWebServerApplicationContext context;

	private AnnotationConfigApplicationContext clientContext;

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
		if (this.clientContext != null) {
			this.clientContext.close();
		}
	}

	@Test
	public void warnIfRestartDisabled() {
		configure("spring.devtools.remote.restart.enabled:false");
		assertThat(this.output.toString()).contains("Remote restart is disabled");
	}

	@Test
	public void warnIfNotHttps() {
		configure("http://localhost", true);
		assertThat(this.output.toString()).contains("is insecure");
	}

	@Test
	public void doesntWarnIfUsingHttps() {
		configure("https://localhost", true);
		assertThat(this.output.toString()).doesNotContain("is insecure");
	}

	@Test
	public void failIfNoSecret() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> configure("http://localhost", false))
				.withMessageContaining("required to secure your connection");
	}

	@Test
	public void liveReloadOnClassPathChanged() throws Exception {
		configure();
		Set<ChangedFiles> changeSet = new HashSet<>();
		ClassPathChangedEvent event = new ClassPathChangedEvent(this, changeSet, false);
		this.clientContext.publishEvent(event);
		LiveReloadConfiguration configuration = this.clientContext
				.getBean(LiveReloadConfiguration.class);
		configuration.getExecutor().shutdown();
		configuration.getExecutor().awaitTermination(2, TimeUnit.SECONDS);
		LiveReloadServer server = this.clientContext.getBean(LiveReloadServer.class);
		verify(server).triggerReload();
	}

	@Test
	public void liveReloadDisabled() {
		configure("spring.devtools.livereload.enabled:false");
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(OptionalLiveReloadServer.class));
	}

	@Test
	public void remoteRestartDisabled() {
		configure("spring.devtools.remote.restart.enabled:false");
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(ClassPathFileSystemWatcher.class));
	}

	private void configure(String... pairs) {
		configure("http://localhost", true, pairs);
	}

	private void configure(String remoteUrl, boolean setSecret, String... pairs) {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(Config.class);
		if (setSecret) {
			TestPropertyValues.of("spring.devtools.remote.secret:secret")
					.applyTo(this.context);
		}
		this.context.refresh();
		this.clientContext = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(pairs).applyTo(this.clientContext);
		new RestartScopeInitializer().initialize(this.clientContext);
		this.clientContext.register(ClientConfig.class, RemoteClientConfiguration.class);
		if (setSecret) {
			TestPropertyValues.of("spring.devtools.remote.secret:secret")
					.applyTo(this.clientContext);
		}
		String remoteUrlProperty = "remoteUrl:" + remoteUrl + ":"
				+ this.context.getWebServer().getPort();
		TestPropertyValues.of(remoteUrlProperty).applyTo(this.clientContext);
		this.clientContext.refresh();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public DispatcherFilter dispatcherFilter() throws IOException {
			return new DispatcherFilter(dispatcher());
		}

		public Dispatcher dispatcher() throws IOException {
			Dispatcher dispatcher = mock(Dispatcher.class);
			ServerHttpRequest anyRequest = any(ServerHttpRequest.class);
			ServerHttpResponse anyResponse = any(ServerHttpResponse.class);
			given(dispatcher.handle(anyRequest, anyResponse)).willReturn(true);
			return dispatcher;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientConfig {

		@Bean
		public LiveReloadServer liveReloadServer() {
			return mock(LiveReloadServer.class);
		}

	}

}
