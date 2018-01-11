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

package org.springframework.boot.autoconfigure.web.reactive;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultReactiveWebServerFactoryCustomizer}.
 *
 * @author Brian Clozel
 */
public class DefaultReactiveWebServerFactoryCustomizerTests {

	private final ServerProperties properties = new ServerProperties();

	private DefaultReactiveWebServerFactoryCustomizer customizer;

	@Before
	public void setup() {
		this.customizer = new DefaultReactiveWebServerFactoryCustomizer(this.properties);
	}

	@Test
	public void testCustomizeServerPort() {
		ConfigurableReactiveWebServerFactory factory = mock(
				ConfigurableReactiveWebServerFactory.class);
		this.properties.setPort(9000);
		this.customizer.customize(factory);
		verify(factory).setPort(9000);
	}

	@Test
	public void testCustomizeServerAddress() {
		ConfigurableReactiveWebServerFactory factory = mock(
				ConfigurableReactiveWebServerFactory.class);
		InetAddress address = mock(InetAddress.class);
		this.properties.setAddress(address);
		this.customizer.customize(factory);
		verify(factory).setAddress(address);
	}

	@Test
	public void tomcatAccessLogIsDisabledByDefault() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		this.customizer.customize(factory);
		assertThat(factory.getEngineValves()).isEmpty();
	}

	@Test
	public void tomcatAccessLogCanBeEnabled() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.enabled", "true");
		bindProperties(map);
		this.customizer.customize(factory);
		assertThat(factory.getEngineValves()).hasSize(1);
		assertThat(factory.getEngineValves()).first().isInstanceOf(AccessLogValve.class);
	}

	@Test
	public void tomcatAccessLogFileDateFormatByDefault() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.enabled", "true");
		bindProperties(map);
		this.customizer.customize(factory);
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next())
				.getFileDateFormat()).isEqualTo(".yyyy-MM-dd");
	}

	@Test
	public void tomcatAccessLogFileDateFormatCanBeRedefined() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.enabled", "true");
		map.put("server.tomcat.accesslog.file-date-format", "yyyy-MM-dd.HH");
		bindProperties(map);
		this.customizer.customize(factory);
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next())
				.getFileDateFormat()).isEqualTo("yyyy-MM-dd.HH");
	}

	@Test
	public void tomcatAccessLogIsBufferedByDefault() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.enabled", "true");
		bindProperties(map);
		this.customizer.customize(factory);
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next())
				.isBuffered()).isTrue();
	}

	@Test
	public void tomcatAccessLogBufferingCanBeDisabled() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.enabled", "true");
		map.put("server.tomcat.accesslog.buffered", "false");
		bindProperties(map);
		this.customizer.customize(factory);
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next())
				.isBuffered()).isFalse();
	}

	@Test
	public void disableTomcatRemoteIpValve() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.remote-ip-header", "");
		map.put("server.tomcat.protocol-header", "");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		this.customizer.customize(factory);
		assertThat(factory.getEngineValves()).isEmpty();
	}

	@Test
	public void defaultTomcatBackgroundProcessorDelay() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		this.customizer.customize(factory);
		TomcatWebServer webServer = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		assertThat(webServer.getTomcat().getEngine().getBackgroundProcessorDelay())
				.isEqualTo(30);
		webServer.stop();
	}

	@Test
	public void customTomcatBackgroundProcessorDelay() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.background-processor-delay", "5");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		this.customizer.customize(factory);
		TomcatWebServer webServer = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		assertThat(webServer.getTomcat().getEngine().getBackgroundProcessorDelay())
				.isEqualTo(5);
		webServer.stop();
	}

	@Test
	public void defaultTomcatRemoteIpValve() {
		Map<String, String> map = new HashMap<>();
		// Since 1.1.7 you need to specify at least the protocol
		map.put("server.tomcat.protocol-header", "X-Forwarded-Proto");
		map.put("server.tomcat.remote-ip-header", "X-Forwarded-For");
		bindProperties(map);
		testRemoteIpValveConfigured();
	}

	@Test
	public void setUseForwardHeadersTomcat() {
		// Since 1.3.0 no need to explicitly set header names if use-forward-header=true
		this.properties.setUseForwardHeaders(true);
		testRemoteIpValveConfigured();
	}

	@Test
	public void deduceUseForwardHeadersTomcat() {
		this.customizer.setEnvironment(new MockEnvironment().withProperty("DYNO", "-"));
		testRemoteIpValveConfigured();
	}

	private void testRemoteIpValveConfigured() {
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		this.customizer.customize(factory);
		assertThat(factory.getEngineValves()).hasSize(1);
		Valve valve = factory.getEngineValves().iterator().next();
		assertThat(valve).isInstanceOf(RemoteIpValve.class);
		RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
		assertThat(remoteIpValve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
		assertThat(remoteIpValve.getProtocolHeaderHttpsValue()).isEqualTo("https");
		assertThat(remoteIpValve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
		String expectedInternalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
				+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
				+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
				+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
				+ "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
				+ "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|"
				+ "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}";
		assertThat(remoteIpValve.getInternalProxies()).isEqualTo(expectedInternalProxies);
	}

	@Test
	public void customTomcatRemoteIpValve() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.remote-ip-header", "x-my-remote-ip-header");
		map.put("server.tomcat.protocol-header", "x-my-protocol-header");
		map.put("server.tomcat.internal-proxies", "192.168.0.1");
		map.put("server.tomcat.port-header", "x-my-forward-port");
		map.put("server.tomcat.protocol-header-https-value", "On");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
		this.customizer.customize(factory);
		assertThat(factory.getEngineValves()).hasSize(1);
		Valve valve = factory.getEngineValves().iterator().next();
		assertThat(valve).isInstanceOf(RemoteIpValve.class);
		RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
		assertThat(remoteIpValve.getProtocolHeader()).isEqualTo("x-my-protocol-header");
		assertThat(remoteIpValve.getProtocolHeaderHttpsValue()).isEqualTo("On");
		assertThat(remoteIpValve.getRemoteIpHeader()).isEqualTo("x-my-remote-ip-header");
		assertThat(remoteIpValve.getPortHeader()).isEqualTo("x-my-forward-port");
		assertThat(remoteIpValve.getInternalProxies()).isEqualTo("192.168.0.1");
	}

	@Test
	public void customTomcatAcceptCount() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accept-count", "10");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory(0);
		this.customizer.customize(factory);
		TomcatWebServer server = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		server.start();
		try {
			assertThat(((AbstractProtocol<?>) server.getTomcat().getConnector()
					.getProtocolHandler()).getAcceptCount()).isEqualTo(10);
		}
		finally {
			server.stop();
		}
	}

	@Test
	public void customTomcatMaxConnections() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.max-connections", "5");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory(0);
		this.customizer.customize(factory);
		TomcatWebServer server = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		server.start();
		try {
			assertThat(((AbstractProtocol<?>) server.getTomcat().getConnector()
					.getProtocolHandler()).getMaxConnections()).isEqualTo(5);
		}
		finally {
			server.stop();
		}
	}

	@Test
	public void customTomcatMaxHttpPostSize() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.max-http-post-size", "10000");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory(0);
		this.customizer.customize(factory);
		TomcatWebServer server = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		server.start();
		try {
			assertThat(server.getTomcat().getConnector().getMaxPostSize())
					.isEqualTo(10000);
		}
		finally {
			server.stop();
		}
	}

	@Test
	public void customTomcatDisableMaxHttpPostSize() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.max-http-post-size", "-1");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory(0);
		this.customizer.customize(factory);
		TomcatWebServer server = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		server.start();
		try {
			assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(-1);
		}
		finally {
			server.stop();
		}
	}

	@Test
	public void testCustomizeTomcatMinSpareThreads() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.min-spare-threads", "10");
		bindProperties(map);
		assertThat(this.properties.getTomcat().getMinSpareThreads()).isEqualTo(10);
	}

	@Test
	public void customTomcatStaticResourceCacheTtl() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.resource.cache-ttl", "10000");
		bindProperties(map);
		TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory(0);
		this.customizer.customize(factory);
		TomcatWebServer server = (TomcatWebServer) factory.getWebServer(mock(HttpHandler.class));
		server.start();
		try {
			Tomcat tomcat = server.getTomcat();
			Context context = (Context) tomcat.getHost().findChildren()[0];
			assertThat(context.getResources().getCacheTtl()).isEqualTo(10000L);
		}
		finally {
			server.stop();
		}
	}

	private void bindProperties(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server", Bindable.ofInstance(this.properties));
	}

}
