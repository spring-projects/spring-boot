/*
 * Copyright 2012-2022 the original author or authors.
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
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.livereload.LiveReloadServer;
import org.springframework.boot.devtools.remote.server.Dispatcher;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.restart.MockRestarter;
import org.springframework.boot.devtools.restart.RestartScopeInitializer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RemoteClientConfiguration}.
 *
 * @author Phillip Webb
 */
@ExtendWith({ OutputCaptureExtension.class, MockRestarter.class })
class RemoteClientConfigurationTests {

	private AnnotationConfigServletWebServerApplicationContext context;

	private AnnotationConfigApplicationContext clientContext;

	@AfterEach
	void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
		if (this.clientContext != null) {
			this.clientContext.close();
		}
	}

	@Test
	void warnIfRestartDisabled(CapturedOutput output) {
		configure("spring.devtools.remote.restart.enabled:false");
		assertThat(output).contains("Remote restart is disabled");
	}

	@Test
	void warnIfNotHttps(CapturedOutput output) {
		configure("http://localhost", true);
		assertThat(output).contains("is insecure");
	}

	@Test
	void doesntWarnIfUsingHttps(CapturedOutput output) {
		configure("https://localhost", true);
		assertThat(output).doesNotContain("is insecure");
	}

	@Test
	void failIfNoSecret() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> configure("http://localhost", false))
				.withMessageContaining("required to secure your connection");
	}

	@Test
	void liveReloadOnClassPathChanged() throws Exception {
		configure();
		Set<ChangedFiles> changeSet = new HashSet<>();
		ClassPathChangedEvent event = new ClassPathChangedEvent(this, changeSet, false);
		this.clientContext.publishEvent(event);
		LiveReloadServer server = this.clientContext.getBean(LiveReloadServer.class);
		Awaitility.await().atMost(Duration.ofMinutes(1)).untilAsserted(() -> then(server).should().triggerReload());
	}

	@Test
	void liveReloadDisabled() {
		configure("spring.devtools.livereload.enabled:false");
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.context.getBean(OptionalLiveReloadServer.class));
	}

	@Test
	void remoteRestartDisabled() {
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
			TestPropertyValues.of("spring.devtools.remote.secret:secret").applyTo(this.context);
		}
		this.context.refresh();
		this.clientContext = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(pairs).applyTo(this.clientContext);
		new RestartScopeInitializer().initialize(this.clientContext);
		this.clientContext.register(ClientConfig.class, RemoteClientConfiguration.class);
		if (setSecret) {
			TestPropertyValues.of("spring.devtools.remote.secret:secret").applyTo(this.clientContext);
		}
		String remoteUrlProperty = "remoteUrl:" + remoteUrl + ":" + this.context.getWebServer().getPort();
		TestPropertyValues.of(remoteUrlProperty).applyTo(this.clientContext);
		this.clientContext.refresh();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		TomcatServletWebServerFactory tomcat() {
			TomcatServletWebServerFactory webServerFactory = new TomcatServletWebServerFactory(0);
			webServerFactory.setRegisterDefaultServlet(true);
			return webServerFactory;
		}

		@Bean
		DispatcherFilter dispatcherFilter() throws IOException {
			return new DispatcherFilter(dispatcher());
		}

		Dispatcher dispatcher() throws IOException {
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
		LiveReloadServer liveReloadServer() {
			return mock(LiveReloadServer.class);
		}

	}

}
