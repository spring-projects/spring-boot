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

package org.springframework.boot.developertools.remote.client;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.developertools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.boot.developertools.classpath.ClassPathChangedEvent;
import org.springframework.boot.developertools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.developertools.filewatch.ChangedFiles;
import org.springframework.boot.developertools.livereload.LiveReloadServer;
import org.springframework.boot.developertools.remote.client.RemoteClientConfiguration.LiveReloadConfiguration;
import org.springframework.boot.developertools.remote.server.Dispatcher;
import org.springframework.boot.developertools.remote.server.DispatcherFilter;
import org.springframework.boot.developertools.restart.MockRestarter;
import org.springframework.boot.developertools.restart.RestartScopeInitializer;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.SocketUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
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

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	private static int remotePort = SocketUtils.findAvailableTcpPort();

	@After
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void warnIfRestartDisabled() throws Exception {
		configure("spring.developertools.remote.restart.enabled:false");
		assertThat(this.output.toString(),
				containsString("Remote restart is not enabled"));
	}

	@Test
	public void doesntWarnIfUsingHttps() throws Exception {
		configureWithRemoteUrl("https://localhost");
		assertThat(this.output.toString(), not(containsString("is insecure")));
	}

	@Test
	public void liveReloadOnClassPathChanged() throws Exception {
		configure();
		Set<ChangedFiles> changeSet = new HashSet<ChangedFiles>();
		ClassPathChangedEvent event = new ClassPathChangedEvent(this, changeSet, false);
		this.context.publishEvent(event);
		LiveReloadConfiguration configuration = this.context
				.getBean(LiveReloadConfiguration.class);
		configuration.getExecutor().shutdown();
		configuration.getExecutor().awaitTermination(2, TimeUnit.SECONDS);
		LiveReloadServer server = this.context.getBean(LiveReloadServer.class);
		verify(server).triggerReload();
	}

	@Test
	public void liveReloadDisabled() throws Exception {
		configure("spring.developertools.livereload.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(OptionalLiveReloadServer.class);
	}

	@Test
	public void remoteRestartDisabled() throws Exception {
		configure("spring.developertools.remote.restart.enabled:false");
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(ClassPathFileSystemWatcher.class);
	}

	private void configure(String... pairs) {
		configureWithRemoteUrl("http://localhost", pairs);
	}

	private void configureWithRemoteUrl(String remoteUrl, String... pairs) {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		new RestartScopeInitializer().initialize(this.context);
		this.context.register(Config.class, RemoteClientConfiguration.class);
		String remoteUrlProperty = "remoteUrl:" + remoteUrl + ":"
				+ RemoteClientConfigurationTests.remotePort;
		EnvironmentTestUtils.addEnvironment(this.context, remoteUrlProperty);
		EnvironmentTestUtils.addEnvironment(this.context, pairs);
		this.context.refresh();
	}

	@Configuration
	static class Config {

		@Bean
		public TomcatEmbeddedServletContainerFactory tomcat() {
			return new TomcatEmbeddedServletContainerFactory(remotePort);
		}

		@Bean
		public LiveReloadServer liveReloadServer() {
			return mock(LiveReloadServer.class);
		}

		@Bean
		public DispatcherFilter dispatcherFilter() throws IOException {
			return new DispatcherFilter(dispatcher());
		}

		public Dispatcher dispatcher() throws IOException {
			Dispatcher dispatcher = mock(Dispatcher.class);
			ServerHttpRequest anyRequest = (ServerHttpRequest) any();
			ServerHttpResponse anyResponse = (ServerHttpResponse) any();
			given(dispatcher.handle(anyRequest, anyResponse)).willReturn(true);
			return dispatcher;
		}

	}

}
