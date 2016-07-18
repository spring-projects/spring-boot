/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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

	@Captor
	private ArgumentCaptor<ServletContextInitializer[]> initializersCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testAddressBinding() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("server.address", "127.0.0.1")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(this.properties.getAddress())
				.isEqualTo(InetAddress.getByName("127.0.0.1"));
	}

	@Test
	public void testPortBinding() throws Exception {
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				Collections.singletonMap("server.port", "9000")));
		assertThat(this.properties.getPort().intValue()).isEqualTo(9000);
	}

	@Test
	public void testServerHeaderDefault() throws Exception {
		assertThat(this.properties.getServerHeader()).isNull();
	}

	@Test
	public void testServerHeader() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("server.server-header", "Custom Server")));
		assertThat(this.properties.getServerHeader()).isEqualTo("Custom Server");
	}

	@Test
	public void testServletPathAsMapping() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("server.servletPath", "/foo/*")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(this.properties.getServletMapping()).isEqualTo("/foo/*");
		assertThat(this.properties.getServletPrefix()).isEqualTo("/foo");
	}

	@Test
	public void testServletPathAsPrefix() throws Exception {
		RelaxedDataBinder binder = new RelaxedDataBinder(this.properties, "server");
		binder.bind(new MutablePropertyValues(
				Collections.singletonMap("server.servletPath", "/foo")));
		assertThat(binder.getBindingResult().hasErrors()).isFalse();
		assertThat(this.properties.getServletMapping()).isEqualTo("/foo/*");
		assertThat(this.properties.getServletPrefix()).isEqualTo("/foo");
	}

	@Test
	public void testTomcatBinding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.accesslog.prefix", "foo");
		map.put("server.tomcat.accesslog.rename-on-rotate", "true");
		map.put("server.tomcat.accesslog.suffix", "-bar.log");
		map.put("server.tomcat.protocol_header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remote_ip_header", "Remote-Ip");
		map.put("server.tomcat.internal_proxies", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		bindProperties(map);
		ServerProperties.Tomcat tomcat = this.properties.getTomcat();
		assertThat(tomcat.getAccesslog().getPattern()).isEqualTo("%h %t '%r' %s %b");
		assertThat(tomcat.getAccesslog().getPrefix()).isEqualTo("foo");
		assertThat(tomcat.getAccesslog().isRenameOnRotate()).isTrue();
		assertThat(tomcat.getAccesslog().getSuffix()).isEqualTo("-bar.log");
		assertThat(tomcat.getRemoteIpHeader()).isEqualTo("Remote-Ip");
		assertThat(tomcat.getProtocolHeader()).isEqualTo("X-Forwarded-Protocol");
		assertThat(tomcat.getInternalProxies())
				.isEqualTo("10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
	}

	@Test
	public void redirectContextRootIsNotConfiguredByDefault() throws Exception {
		bindProperties(new HashMap<String, String>());
		ServerProperties.Tomcat tomcat = this.properties.getTomcat();
		assertThat(tomcat.getRedirectContextRoot()).isNull();
	}

	@Test
	public void redirectContextRootCanBeConfigured() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.redirect-context-root", "false");
		bindProperties(map);
		ServerProperties.Tomcat tomcat = this.properties.getTomcat();
		assertThat(tomcat.getRedirectContextRoot()).isEqualTo(false);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);
		Context context = mock(Context.class);
		for (TomcatContextCustomizer customizer : container
				.getTomcatContextCustomizers()) {
			customizer.customize(context);
		}
		verify(context).setMapperContextRootRedirectEnabled(false);
	}

	@Test
	public void testTrailingSlashOfContextPathIsRemoved() {
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				Collections.singletonMap("server.contextPath", "/foo/")));
		assertThat(this.properties.getContextPath()).isEqualTo("/foo");
	}

	@Test
	public void testSlashOfContextPathIsDefaultValue() {
		new RelaxedDataBinder(this.properties, "server").bind(new MutablePropertyValues(
				Collections.singletonMap("server.contextPath", "/")));
		assertThat(this.properties.getContextPath()).isEqualTo("");
	}

	@Test
	public void testCustomizeTomcat() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(
				ConfigurableEmbeddedServletContainer.class);
		this.properties.customize(factory);
		verify(factory, never()).setContextPath("");
	}

	@Test
	public void testDefaultDisplayName() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(
				ConfigurableEmbeddedServletContainer.class);
		this.properties.customize(factory);
		verify(factory).setDisplayName("application");
	}

	@Test
	public void testCustomizeDisplayName() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(
				ConfigurableEmbeddedServletContainer.class);
		this.properties.setDisplayName("TestName");
		this.properties.customize(factory);
		verify(factory).setDisplayName("TestName");
	}

	@Test
	public void customizeSessionProperties() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.session.timeout", "123");
		map.put("server.session.tracking-modes", "cookie,url");
		map.put("server.session.cookie.name", "testname");
		map.put("server.session.cookie.domain", "testdomain");
		map.put("server.session.cookie.path", "/testpath");
		map.put("server.session.cookie.comment", "testcomment");
		map.put("server.session.cookie.http-only", "true");
		map.put("server.session.cookie.secure", "true");
		map.put("server.session.cookie.max-age", "60");
		bindProperties(map);
		ConfigurableEmbeddedServletContainer factory = mock(
				ConfigurableEmbeddedServletContainer.class);
		ServletContext servletContext = mock(ServletContext.class);
		SessionCookieConfig sessionCookieConfig = mock(SessionCookieConfig.class);
		given(servletContext.getSessionCookieConfig()).willReturn(sessionCookieConfig);
		this.properties.customize(factory);
		triggerInitializers(factory, servletContext);
		verify(factory).setSessionTimeout(123);
		verify(servletContext).setSessionTrackingModes(
				EnumSet.of(SessionTrackingMode.COOKIE, SessionTrackingMode.URL));
		verify(sessionCookieConfig).setName("testname");
		verify(sessionCookieConfig).setDomain("testdomain");
		verify(sessionCookieConfig).setPath("/testpath");
		verify(sessionCookieConfig).setComment("testcomment");
		verify(sessionCookieConfig).setHttpOnly(true);
		verify(sessionCookieConfig).setSecure(true);
		verify(sessionCookieConfig).setMaxAge(60);
	}

	private void triggerInitializers(ConfigurableEmbeddedServletContainer container,
			ServletContext servletContext) throws ServletException {
		verify(container, atLeastOnce())
				.addInitializers(this.initializersCaptor.capture());
		for (Object initializers : this.initializersCaptor.getAllValues()) {
			if (initializers instanceof ServletContextInitializer) {
				((ServletContextInitializer) initializers).onStartup(servletContext);
			}
			else {
				for (ServletContextInitializer initializer : (ServletContextInitializer[]) initializers) {
					initializer.onStartup(servletContext);
				}
			}
		}
	}

	@Test
	public void testCustomizeTomcatPort() throws Exception {
		ConfigurableEmbeddedServletContainer factory = mock(
				ConfigurableEmbeddedServletContainer.class);
		this.properties.setPort(8080);
		this.properties.customize(factory);
		verify(factory).setPort(8080);
	}

	@Test
	public void testCustomizeUriEncoding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.uriEncoding", "US-ASCII");
		bindProperties(map);
		assertThat(this.properties.getTomcat().getUriEncoding())
				.isEqualTo(Charset.forName("US-ASCII"));
	}

	@Test
	public void testCustomizeHeaderSize() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.maxHttpHeaderSize", "9999");
		bindProperties(map);
		assertThat(this.properties.getMaxHttpHeaderSize()).isEqualTo(9999);
	}

	@Test
	public void testCustomizePostSize() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.maxHttpPostSize", "9999");
		bindProperties(map);
		assertThat(this.properties.getMaxHttpPostSize()).isEqualTo(9999);
	}

	@Test
	public void testCustomizeJettyAcceptors() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.jetty.acceptors", "10");
		bindProperties(map);
		assertThat(this.properties.getJetty().getAcceptors()).isEqualTo(10);
	}

	@Test
	public void testCustomizeJettySelectors() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.jetty.selectors", "10");
		bindProperties(map);
		assertThat(this.properties.getJetty().getSelectors()).isEqualTo(10);
	}

	@Test
	public void testCustomizeTomcatMinSpareThreads() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.min-spare-threads", "10");
		bindProperties(map);
		assertThat(this.properties.getTomcat().getMinSpareThreads()).isEqualTo(10);
	}

	@Test
	public void customizeTomcatDisplayName() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.display-name", "MyBootApp");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);
		assertThat(container.getDisplayName()).isEqualTo("MyBootApp");
	}

	@Test
	public void disableTomcatRemoteIpValve() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.remote_ip_header", "");
		map.put("server.tomcat.protocol_header", "");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);
		assertThat(container.getEngineValves()).isEmpty();
	}

	@Test
	public void defaultTomcatRemoteIpValve() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		// Since 1.1.7 you need to specify at least the protocol
		map.put("server.tomcat.protocol_header", "X-Forwarded-Proto");
		map.put("server.tomcat.remote_ip_header", "X-Forwarded-For");
		bindProperties(map);
		testRemoteIpValveConfigured();
	}

	@Test
	public void setUseForwardHeadersTomcat() throws Exception {
		// Since 1.3.0 no need to explicitly set header names if use-forward-header=true
		this.properties.setUseForwardHeaders(true);
		testRemoteIpValveConfigured();
	}

	@Test
	public void deduceUseForwardHeadersTomcat() throws Exception {
		this.properties.setEnvironment(new MockEnvironment().withProperty("DYNO", "-"));
		testRemoteIpValveConfigured();
	}

	private void testRemoteIpValveConfigured() {
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);
		assertThat(container.getEngineValves()).hasSize(1);
		Valve valve = container.getEngineValves().iterator().next();
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
	public void customTomcatRemoteIpValve() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.remote_ip_header", "x-my-remote-ip-header");
		map.put("server.tomcat.protocol_header", "x-my-protocol-header");
		map.put("server.tomcat.internal_proxies", "192.168.0.1");
		map.put("server.tomcat.port-header", "x-my-forward-port");
		map.put("server.tomcat.protocol-header-https-value", "On");
		bindProperties(map);

		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);

		assertThat(container.getEngineValves()).hasSize(1);
		Valve valve = container.getEngineValves().iterator().next();
		assertThat(valve).isInstanceOf(RemoteIpValve.class);
		RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
		assertThat(remoteIpValve.getProtocolHeader()).isEqualTo("x-my-protocol-header");
		assertThat(remoteIpValve.getProtocolHeaderHttpsValue()).isEqualTo("On");
		assertThat(remoteIpValve.getRemoteIpHeader()).isEqualTo("x-my-remote-ip-header");
		assertThat(remoteIpValve.getPortHeader()).isEqualTo("x-my-forward-port");
		assertThat(remoteIpValve.getInternalProxies()).isEqualTo("192.168.0.1");
	}

	@Test
	public void defaultUseForwardHeadersUndertow() throws Exception {
		UndertowEmbeddedServletContainerFactory container = spy(
				new UndertowEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setUseForwardHeaders(false);
	}

	@Test
	public void setUseForwardHeadersUndertow() throws Exception {
		this.properties.setUseForwardHeaders(true);
		UndertowEmbeddedServletContainerFactory container = spy(
				new UndertowEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setUseForwardHeaders(true);
	}

	@Test
	public void deduceUseForwardHeadersUndertow() throws Exception {
		this.properties.setEnvironment(new MockEnvironment().withProperty("DYNO", "-"));
		UndertowEmbeddedServletContainerFactory container = spy(
				new UndertowEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setUseForwardHeaders(true);
	}

	@Test
	public void defaultUseForwardHeadersJetty() throws Exception {
		JettyEmbeddedServletContainerFactory container = spy(
				new JettyEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setUseForwardHeaders(false);
	}

	@Test
	public void setUseForwardHeadersJetty() throws Exception {
		this.properties.setUseForwardHeaders(true);
		JettyEmbeddedServletContainerFactory container = spy(
				new JettyEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setUseForwardHeaders(true);
	}

	@Test
	public void deduceUseForwardHeadersJetty() throws Exception {
		this.properties.setEnvironment(new MockEnvironment().withProperty("DYNO", "-"));
		JettyEmbeddedServletContainerFactory container = spy(
				new JettyEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setUseForwardHeaders(true);
	}

	@Test
	public void sessionStoreDir() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.session.store-dir", "myfolder");
		bindProperties(map);
		JettyEmbeddedServletContainerFactory container = spy(
				new JettyEmbeddedServletContainerFactory());
		this.properties.customize(container);
		verify(container).setSessionStoreDir(new File("myfolder"));
	}

	@Test
	public void skipNullElementsForUndertow() throws Exception {
		UndertowEmbeddedServletContainerFactory container = mock(
				UndertowEmbeddedServletContainerFactory.class);
		this.properties.customize(container);
		verify(container, never()).setAccessLogEnabled(anyBoolean());
	}

	private void bindProperties(Map<String, String> map) {
		new RelaxedDataBinder(this.properties, "server")
				.bind(new MutablePropertyValues(map));
	}

}
