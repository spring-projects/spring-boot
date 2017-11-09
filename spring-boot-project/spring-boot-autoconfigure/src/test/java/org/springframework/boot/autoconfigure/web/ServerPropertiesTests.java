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

package org.springframework.boot.autoconfigure.web;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

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
	public void testPortBinding() throws Exception {
		bind("server.port", "9000");
		assertThat(this.properties.getPort().intValue()).isEqualTo(9000);
	}

	@Test
	public void testServerHeaderDefault() throws Exception {
		assertThat(this.properties.getServerHeader()).isNull();
	}

	@Test
	public void testServerHeader() throws Exception {
		bind("server.server-header", "Custom Server");
		assertThat(this.properties.getServerHeader()).isEqualTo("Custom Server");
	}

	@Test
	public void testConnectionTimeout() throws Exception {
		bind("server.connection-timeout", "60000");
		assertThat(this.properties.getConnectionTimeout()).isEqualTo(60000);
	}

	@Test
	public void testServletPathAsMapping() throws Exception {
		bind("server.servlet.path", "/foo/*");
		assertThat(this.properties.getServlet().getServletMapping()).isEqualTo("/foo/*");
		assertThat(this.properties.getServlet().getServletPrefix()).isEqualTo("/foo");
	}

	@Test
	public void testServletPathAsPrefix() throws Exception {
		bind("server.servlet.path", "/foo");
		assertThat(this.properties.getServlet().getServletMapping()).isEqualTo("/foo/*");
		assertThat(this.properties.getServlet().getServletPrefix()).isEqualTo("/foo");
	}

	@Test
	public void testTomcatBinding() throws Exception {
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
		assertThat(tomcat.getBackgroundProcessorDelay()).isEqualTo(10);
	}

	@Test
	public void redirectContextRootIsNotConfiguredByDefault() throws Exception {
		bind(new HashMap<>());
		ServerProperties.Tomcat tomcat = this.properties.getTomcat();
		assertThat(tomcat.getRedirectContextRoot()).isNull();
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
	public void testCustomizeUriEncoding() throws Exception {
		bind("server.tomcat.uri-encoding", "US-ASCII");
		assertThat(this.properties.getTomcat().getUriEncoding())
				.isEqualTo(StandardCharsets.US_ASCII);
	}

	@Test
	public void testCustomizeHeaderSize() throws Exception {
		bind("server.max-http-header-size", "9999");
		assertThat(this.properties.getMaxHttpHeaderSize()).isEqualTo(9999);
	}

	@Test
	public void testCustomizeJettyAcceptors() throws Exception {
		bind("server.jetty.acceptors", "10");
		assertThat(this.properties.getJetty().getAcceptors()).isEqualTo(10);
	}

	@Test
	public void testCustomizeJettySelectors() throws Exception {
		bind("server.jetty.selectors", "10");
		assertThat(this.properties.getJetty().getSelectors()).isEqualTo(10);
	}

	@Test
	public void testCustomizeJettyAccessLog() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("server.jetty.accesslog.enabled", "true");
		map.put("server.jetty.accesslog.filename", "foo.txt");
		map.put("server.jetty.accesslog.file-date-format", "yyyymmdd");
		map.put("server.jetty.accesslog.retention-period", "4");
		map.put("server.jetty.accesslog.append", "true");
		bind(map);
		ServerProperties.Jetty jetty = this.properties.getJetty();
		assertThat(jetty.getAccesslog().isEnabled()).isEqualTo(true);
		assertThat(jetty.getAccesslog().getFilename()).isEqualTo("foo.txt");
		assertThat(jetty.getAccesslog().getFileDateFormat()).isEqualTo("yyyymmdd");
		assertThat(jetty.getAccesslog().getRetentionPeriod()).isEqualTo(4);
		assertThat(jetty.getAccesslog().isAppend()).isEqualTo(true);
	}

	private void bind(String name, String value) {
		bind(Collections.singletonMap(name, value));
	}

	private void bind(Map<String, String> map) {
		ConfigurationPropertySource source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("server", Bindable.ofInstance(this.properties));
	}

}
