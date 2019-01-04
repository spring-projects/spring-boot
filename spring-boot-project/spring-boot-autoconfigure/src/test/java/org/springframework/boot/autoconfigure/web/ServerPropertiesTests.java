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

package org.springframework.boot.autoconfigure.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.UndertowOptions;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.junit.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerProperties}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 */
public class ServerPropertiesTests {

	private final ServerProperties properties = new ServerProperties();

	@Test
	public void testAddressBinding() throws Exception {
		bind("server.address", "127.0.0.1");
		assertThat(this.properties.getAddress())
				.isEqualTo(InetAddress.getByName("127.0.0.1"));
	}

	@Test
	public void testPortBinding() {
		bind("server.port", "9000");
		assertThat(this.properties.getPort().intValue()).isEqualTo(9000);
	}

	@Test
	public void testServerHeaderDefault() {
		assertThat(this.properties.getServerHeader()).isNull();
	}

	@Test
	public void testServerHeader() {
		bind("server.server-header", "Custom Server");
		assertThat(this.properties.getServerHeader()).isEqualTo("Custom Server");
	}

	@Test
	public void testConnectionTimeout() {
		bind("server.connection-timeout", "60s");
		assertThat(this.properties.getConnectionTimeout())
				.isEqualTo(Duration.ofMillis(60000));
	}

	@Test
	public void testTomcatBinding() {
		Map<String, String> map = new HashMap<>();
		map.put("server.tomcat.accesslog.pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.accesslog.prefix", "foo");
		map.put("server.tomcat.accesslog.rotate", "false");
		map.put("server.tomcat.accesslog.rename-on-rotate", "true");
		map.put("server.tomcat.accesslog.request-attributes-enabled", "true");
		map.put("server.tomcat.accesslog.suffix", "-bar.log");
		map.put("server.tomcat.protocol-header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remote-ip-header", "Remote-Ip");
		map.put("server.tomcat.internal-proxies", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		map.put("server.tomcat.background-processor-delay", "10");
		bind(map);
		ServerProperties.Tomcat tomcat = this.properties.getTomcat();
		assertThat(tomcat.getAccesslog().getPattern()).isEqualTo("%h %t '%r' %s %b");
		assertThat(tomcat.getAccesslog().getPrefix()).isEqualTo("foo");
		assertThat(tomcat.getAccesslog().isRotate()).isFalse();
		assertThat(tomcat.getAccesslog().isRenameOnRotate()).isTrue();
		assertThat(tomcat.getAccesslog().isRequestAttributesEnabled()).isTrue();
		assertThat(tomcat.getAccesslog().getSuffix()).isEqualTo("-bar.log");
		assertThat(tomcat.getRemoteIpHeader()).isEqualTo("Remote-Ip");
		assertThat(tomcat.getProtocolHeader()).isEqualTo("X-Forwarded-Protocol");
		assertThat(tomcat.getInternalProxies())
				.isEqualTo("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		assertThat(tomcat.getBackgroundProcessorDelay())
				.isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	public void testTrailingSlashOfContextPathIsRemoved() {
		bind("server.servlet.context-path", "/foo/");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("/foo");
	}

	@Test
	public void testSlashOfContextPathIsDefaultValue() {
		bind("server.servlet.context-path", "/");
		assertThat(this.properties.getServlet().getContextPath()).isEqualTo("");
	}

	@Test
	public void testCustomizeUriEncoding() {
		bind("server.tomcat.uri-encoding", "US-ASCII");
		assertThat(this.properties.getTomcat().getUriEncoding())
				.isEqualTo(StandardCharsets.US_ASCII);
	}

	@Test
	public void testCustomizeHeaderSize() {
		bind("server.max-http-header-size", "1MB");
		assertThat(this.properties.getMaxHttpHeaderSize())
				.isEqualTo(DataSize.ofMegabytes(1));
	}

	@Test
	public void testCustomizeHeaderSizeUseBytesByDefault() {
		bind("server.max-http-header-size", "1024");
		assertThat(this.properties.getMaxHttpHeaderSize())
				.isEqualTo(DataSize.ofKilobytes(1));
	}

	@Test
	public void testCustomizeJettyAcceptors() {
		bind("server.jetty.acceptors", "10");
		assertThat(this.properties.getJetty().getAcceptors()).isEqualTo(10);
	}

	@Test
	public void testCustomizeJettySelectors() {
		bind("server.jetty.selectors", "10");
		assertThat(this.properties.getJetty().getSelectors()).isEqualTo(10);
	}

	@Test
	public void testCustomizeJettyAccessLog() {
		Map<String, String> map = new HashMap<>();
		map.put("server.jetty.accesslog.enabled", "true");
		map.put("server.jetty.accesslog.filename", "foo.txt");
		map.put("server.jetty.accesslog.file-date-format", "yyyymmdd");
		map.put("server.jetty.accesslog.retention-period", "4");
		map.put("server.jetty.accesslog.append", "true");
		bind(map);
		ServerProperties.Jetty jetty = this.properties.getJetty();
		assertThat(jetty.getAccesslog().isEnabled()).isTrue();
		assertThat(jetty.getAccesslog().getFilename()).isEqualTo("foo.txt");
		assertThat(jetty.getAccesslog().getFileDateFormat()).isEqualTo("yyyymmdd");
		assertThat(jetty.getAccesslog().getRetentionPeriod()).isEqualTo(4);
		assertThat(jetty.getAccesslog().isAppend()).isTrue();
	}

	@Test
	public void tomcatAcceptCountMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getTomcat().getAcceptCount())
				.isEqualTo(getDefaultProtocol().getAcceptCount());
	}

	@Test
	public void tomcatProcessorCacheMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getTomcat().getProcessorCache())
				.isEqualTo(getDefaultProtocol().getProcessorCache());
	}

	@Test
	public void tomcatMaxConnectionsMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getTomcat().getMaxConnections())
				.isEqualTo(getDefaultProtocol().getMaxConnections());
	}

	@Test
	public void tomcatMaxThreadsMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getTomcat().getMaxThreads())
				.isEqualTo(getDefaultProtocol().getMaxThreads());
	}

	@Test
	public void tomcatMinSpareThreadsMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getTomcat().getMinSpareThreads())
				.isEqualTo(getDefaultProtocol().getMinSpareThreads());
	}

	@Test
	public void tomcatMaxHttpPostSizeMatchesConnectorDefault() throws Exception {
		assertThat(this.properties.getTomcat().getMaxHttpPostSize().toBytes())
				.isEqualTo(getDefaultConnector().getMaxPostSize());
	}

	@Test
	public void tomcatBackgroundProcessorDelayMatchesEngineDefault() {
		assertThat(this.properties.getTomcat().getBackgroundProcessorDelay()).isEqualTo(
				Duration.ofSeconds((new StandardEngine().getBackgroundProcessorDelay())));
	}

	@Test
	public void tomcatUriEncodingMatchesConnectorDefault() throws Exception {
		assertThat(this.properties.getTomcat().getUriEncoding().name())
				.isEqualTo(getDefaultConnector().getURIEncoding());
	}

	@Test
	public void tomcatRedirectContextRootMatchesDefault() {
		assertThat(this.properties.getTomcat().getRedirectContextRoot())
				.isEqualTo(new StandardContext().getMapperContextRootRedirectEnabled());
	}

	@Test
	public void tomcatAccessLogRenameOnRotateMatchesDefault() {
		assertThat(this.properties.getTomcat().getAccesslog().isRenameOnRotate())
				.isEqualTo(new AccessLogValve().isRenameOnRotate());
	}

	@Test
	public void tomcatAccessLogRequestAttributesEnabledMatchesDefault() {
		assertThat(
				this.properties.getTomcat().getAccesslog().isRequestAttributesEnabled())
						.isEqualTo(new AccessLogValve().getRequestAttributesEnabled());
	}

	@Test
	public void tomcatInternalProxiesMatchesDefault() {
		assertThat(this.properties.getTomcat().getInternalProxies())
				.isEqualTo(new RemoteIpValve().getInternalProxies());
	}

	@Test
	public void jettyMaxHttpPostSizeMatchesDefault() throws Exception {
		JettyServletWebServerFactory jettyFactory = new JettyServletWebServerFactory(0);
		JettyWebServer jetty = (JettyWebServer) jettyFactory.getWebServer(
				(ServletContextInitializer) (servletContext) -> servletContext
						.addServlet("formPost", new HttpServlet() {

							@Override
							protected void doPost(HttpServletRequest req,
									HttpServletResponse resp)
									throws ServletException, IOException {
								req.getParameterMap();
							}

						}).addMapping("/form"));
		jetty.start();
		org.eclipse.jetty.server.Connector connector = jetty.getServer()
				.getConnectors()[0];
		final AtomicReference<Throwable> failure = new AtomicReference<>();
		connector.addBean(new HttpChannel.Listener() {

			@Override
			public void onDispatchFailure(Request request, Throwable ex) {
				failure.set(ex);
			}

		});
		try {
			RestTemplate template = new RestTemplate();
			template.setErrorHandler(new ResponseErrorHandler() {

				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return false;
				}

				@Override
				public void handleError(ClientHttpResponse response) throws IOException {

				}

			});
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			StringBuilder data = new StringBuilder();
			for (int i = 0; i < 250000; i++) {
				data.append("a");
			}
			body.add("data", data.toString());
			HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body,
					headers);
			template.postForEntity(
					URI.create("http://localhost:" + jetty.getPort() + "/form"), entity,
					Void.class);
			assertThat(failure.get()).isNotNull();
			String message = failure.get().getCause().getMessage();
			int defaultMaxPostSize = Integer
					.valueOf(message.substring(message.lastIndexOf(' ')).trim());
			assertThat(this.properties.getJetty().getMaxHttpPostSize().toBytes())
					.isEqualTo(defaultMaxPostSize);
		}
		finally {
			jetty.stop();
		}
	}

	@Test
	public void undertowMaxHttpPostSizeMatchesDefault() {
		assertThat(this.properties.getUndertow().getMaxHttpPostSize().toBytes())
				.isEqualTo(UndertowOptions.DEFAULT_MAX_ENTITY_SIZE);
	}

	private Connector getDefaultConnector() throws Exception {
		return new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
	}

	private AbstractProtocol<?> getDefaultProtocol() throws Exception {
		return (AbstractProtocol<?>) Class
				.forName(TomcatServletWebServerFactory.DEFAULT_PROTOCOL).newInstance();
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server", Bindable.ofInstance(this.properties));
	}

}
