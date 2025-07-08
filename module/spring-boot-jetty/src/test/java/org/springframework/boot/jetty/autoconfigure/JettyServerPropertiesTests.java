/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jetty.autoconfigure;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyServerProperties}.
 *
 * @author Andy Wilkinson
 */
class JettyServerPropertiesTests {

	private final JettyServerProperties properties = new JettyServerProperties();

	@Test
	void testCustomizeJettyAcceptors() {
		bind("server.jetty.threads.acceptors", "10");
		assertThat(this.properties.getThreads().getAcceptors()).isEqualTo(10);
	}

	@Test
	void testCustomizeJettySelectors() {
		bind("server.jetty.threads.selectors", "10");
		assertThat(this.properties.getThreads().getSelectors()).isEqualTo(10);
	}

	@Test
	void testCustomizeJettyMaxThreads() {
		bind("server.jetty.threads.max", "10");
		assertThat(this.properties.getThreads().getMax()).isEqualTo(10);
	}

	@Test
	void testCustomizeJettyMinThreads() {
		bind("server.jetty.threads.min", "10");
		assertThat(this.properties.getThreads().getMin()).isEqualTo(10);
	}

	@Test
	void testCustomizeJettyIdleTimeout() {
		bind("server.jetty.threads.idle-timeout", "10s");
		assertThat(this.properties.getThreads().getIdleTimeout()).isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	void testCustomizeJettyMaxQueueCapacity() {
		bind("server.jetty.threads.max-queue-capacity", "5150");
		assertThat(this.properties.getThreads().getMaxQueueCapacity()).isEqualTo(5150);
	}

	@Test
	void testCustomizeJettyAccessLog() {
		Map<String, String> map = new HashMap<>();
		map.put("server.jetty.accesslog.enabled", "true");
		map.put("server.jetty.accesslog.filename", "foo.txt");
		map.put("server.jetty.accesslog.file-date-format", "yyyymmdd");
		map.put("server.jetty.accesslog.retention-period", "4");
		map.put("server.jetty.accesslog.append", "true");
		map.put("server.jetty.accesslog.custom-format", "{client}a - %u %t \"%r\" %s %O");
		map.put("server.jetty.accesslog.ignore-paths", "/a/path,/b/path");
		bind(map);
		assertThat(this.properties.getAccesslog().isEnabled()).isTrue();
		assertThat(this.properties.getAccesslog().getFilename()).isEqualTo("foo.txt");
		assertThat(this.properties.getAccesslog().getFileDateFormat()).isEqualTo("yyyymmdd");
		assertThat(this.properties.getAccesslog().getRetentionPeriod()).isEqualTo(4);
		assertThat(this.properties.getAccesslog().isAppend()).isTrue();
		assertThat(this.properties.getAccesslog().getCustomFormat()).isEqualTo("{client}a - %u %t \"%r\" %s %O");
		assertThat(this.properties.getAccesslog().getIgnorePaths()).containsExactly("/a/path", "/b/path");
	}

	@Test
	void jettyThreadPoolPropertyDefaultsShouldMatchServerDefault() {
		JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
		JettyWebServer jetty = (JettyWebServer) jettyFactory.getWebServer();
		Server server = jetty.getServer();
		QueuedThreadPool threadPool = (QueuedThreadPool) server.getThreadPool();
		int idleTimeout = threadPool.getIdleTimeout();
		int maxThreads = threadPool.getMaxThreads();
		int minThreads = threadPool.getMinThreads();
		assertThat(this.properties.getThreads().getIdleTimeout().toMillis()).isEqualTo(idleTimeout);
		assertThat(this.properties.getThreads().getMax()).isEqualTo(maxThreads);
		assertThat(this.properties.getThreads().getMin()).isEqualTo(minThreads);
	}

	@Test
	void jettyMaxHttpFormPostSizeMatchesDefault() {
		JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
		JettyWebServer jetty = (JettyWebServer) jettyFactory.getWebServer();
		Server server = jetty.getServer();
		assertThat(this.properties.getMaxHttpFormPostSize().toBytes())
			.isEqualTo(((ServletContextHandler) server.getHandler()).getMaxFormContentSize());
	}

	@Test
	void jettyMaxFormKeysMatchesDefault() {
		JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
		JettyWebServer jetty = (JettyWebServer) jettyFactory.getWebServer();
		Server server = jetty.getServer();
		assertThat(this.properties.getMaxFormKeys())
			.isEqualTo(((ServletContextHandler) server.getHandler()).getMaxFormKeys());
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server.jetty", Bindable.ofInstance(this.properties));
	}

}
