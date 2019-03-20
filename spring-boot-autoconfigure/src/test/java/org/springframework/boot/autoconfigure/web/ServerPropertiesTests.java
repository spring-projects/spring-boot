/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.undertow.UndertowOptions;
import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
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
	public void testConnectionTimeout() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.connection-timeout", "60000");
		bindProperties(map);
		assertThat(this.properties.getConnectionTimeout()).isEqualTo(60000);
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
	public void tomcatAccessLogIsDisabledByDefault() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(tomcatContainer);
		assertThat(tomcatContainer.getEngineValves()).isEmpty();
	}

	@Test
	public void tomcatAccessLogCanBeEnabled() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.enabled", "true");
		bindProperties(map);
		this.properties.customize(tomcatContainer);
		assertThat(tomcatContainer.getEngineValves()).hasSize(1);
		assertThat(tomcatContainer.getEngineValves()).first()
				.isInstanceOf(AccessLogValve.class);
	}

	@Test
	public void tomcatAccessLogFileDateFormatByDefault() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.enabled", "true");
		bindProperties(map);
		this.properties.customize(tomcatContainer);
		assertThat(((AccessLogValve) tomcatContainer.getEngineValves().iterator().next())
				.getFileDateFormat()).isEqualTo(".yyyy-MM-dd");
	}

	@Test
	public void tomcatAccessLogFileDateFormatCanBeRedefined() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.enabled", "true");
		map.put("server.tomcat.accesslog.file-date-format", "yyyy-MM-dd.HH");
		bindProperties(map);
		this.properties.customize(tomcatContainer);
		assertThat(((AccessLogValve) tomcatContainer.getEngineValves().iterator().next())
				.getFileDateFormat()).isEqualTo("yyyy-MM-dd.HH");
	}

	@Test
	public void tomcatAccessLogIsBufferedByDefault() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.enabled", "true");
		bindProperties(map);
		this.properties.customize(tomcatContainer);
		assertThat(((AccessLogValve) tomcatContainer.getEngineValves().iterator().next())
				.isBuffered()).isTrue();
	}

	@Test
	public void tomcatAccessLogBufferingCanBeDisabled() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.enabled", "true");
		map.put("server.tomcat.accesslog.buffered", "false");
		bindProperties(map);
		this.properties.customize(tomcatContainer);
		assertThat(((AccessLogValve) tomcatContainer.getEngineValves().iterator().next())
				.isBuffered()).isFalse();
	}

	@Test
	public void testTomcatBinding() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accesslog.pattern", "%h %t '%r' %s %b");
		map.put("server.tomcat.accesslog.prefix", "foo");
		map.put("server.tomcat.accesslog.rotate", "false");
		map.put("server.tomcat.accesslog.rename-on-rotate", "true");
		map.put("server.tomcat.accesslog.request-attributes-enabled", "true");
		map.put("server.tomcat.accesslog.suffix", "-bar.log");
		map.put("server.tomcat.protocol_header", "X-Forwarded-Protocol");
		map.put("server.tomcat.remote_ip_header", "Remote-Ip");
		map.put("server.tomcat.internal_proxies", "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
		map.put("server.tomcat.background_processor_delay", "10");
		bindProperties(map);
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
	public void errorReportValveIsConfiguredToNotReportStackTraces() {
		TomcatEmbeddedServletContainerFactory tomcatContainer = new TomcatEmbeddedServletContainerFactory();
		Map<String, String> map = new HashMap<String, String>();
		bindProperties(map);
		this.properties.customize(tomcatContainer);
		Valve[] valves = ((TomcatEmbeddedServletContainer) tomcatContainer
				.getEmbeddedServletContainer()).getTomcat().getHost().getPipeline()
						.getValves();
		assertThat(valves).hasAtLeastOneElementOfType(ErrorReportValve.class);
		for (Valve valve : valves) {
			if (valve instanceof ErrorReportValve) {
				ErrorReportValve errorReportValve = (ErrorReportValve) valve;
				assertThat(errorReportValve.isShowReport()).isFalse();
				assertThat(errorReportValve.isShowServerInfo()).isFalse();
			}
		}
	}

	@Test
	public void redirectContextRootCanBeConfigured() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.redirect-context-root", "false");
		bindProperties(map);
		ServerProperties.Tomcat tomcat = this.properties.getTomcat();
		assertThat(tomcat.getRedirectContextRoot()).isEqualTo(false);
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		Context context = (Context) ((TomcatEmbeddedServletContainer) factory
				.getEmbeddedServletContainer()).getTomcat().getHost().findChildren()[0];
		assertThat(context.getMapperContextRootRedirectEnabled()).isTrue();
		this.properties.customize(factory);
		context = (Context) ((TomcatEmbeddedServletContainer) factory
				.getEmbeddedServletContainer()).getTomcat().getHost().findChildren()[0];
		assertThat(context.getMapperContextRootRedirectEnabled()).isFalse();
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
	public void customizeSessionPropertiesWithJetty() throws Exception {
		customizeSessionProperties(new TomcatEmbeddedServletContainerFactory(0));
	}

	@Test
	public void customizeSessionPropertiesWithTomcat() throws Exception {
		customizeSessionProperties(new TomcatEmbeddedServletContainerFactory(0));
	}

	@Test
	public void customizeSessionPropertiesWithUndertow() throws Exception {
		customizeSessionProperties(new UndertowEmbeddedServletContainerFactory(0));
	}

	private void customizeSessionProperties(
			AbstractEmbeddedServletContainerFactory factory) {
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
		this.properties.customize(factory);
		final AtomicReference<ServletContext> servletContextReference = new AtomicReference<ServletContext>();
		EmbeddedServletContainer container = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {

					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						servletContextReference.set(servletContext);
					}

				});
		try {
			container.start();
			SessionCookieConfig sessionCookieConfig = servletContextReference.get()
					.getSessionCookieConfig();
			assertThat(factory.getSessionTimeout()).isEqualTo(123);
			assertThat(sessionCookieConfig.getName()).isEqualTo("testname");
			assertThat(sessionCookieConfig.getDomain()).isEqualTo("testdomain");
			assertThat(sessionCookieConfig.getPath()).isEqualTo("/testpath");
			assertThat(sessionCookieConfig.getComment()).isEqualTo("testcomment");
			assertThat(sessionCookieConfig.isHttpOnly()).isTrue();
			assertThat(sessionCookieConfig.isSecure()).isTrue();
			assertThat(sessionCookieConfig.getMaxAge()).isEqualTo(60);
			assertThat(servletContextReference.get().getEffectiveSessionTrackingModes())
					.isEqualTo(EnumSet.of(SessionTrackingMode.COOKIE,
							SessionTrackingMode.URL));
		}
		finally {
			container.stop();
		}
	}

	@Test
	public void sslSessionTrackingWithJetty() throws Exception {
		sslSessionTracking(new JettyEmbeddedServletContainerFactory(0));
	}

	@Test
	public void sslSessionTrackingWithTomcat() throws Exception {
		sslSessionTracking(new TomcatEmbeddedServletContainerFactory(0));
	}

	@Test
	public void sslSessionTrackingWithUndertow() throws Exception {
		sslSessionTracking(new UndertowEmbeddedServletContainerFactory(0));
	}

	private void sslSessionTracking(AbstractEmbeddedServletContainerFactory factory) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.ssl.enabled", "true");
		map.put("server.ssl.key-store", "src/test/resources/test.jks");
		map.put("server.ssl.key-password", "password");
		map.put("server.session.tracking-modes", "ssl");
		bindProperties(map);
		this.properties.customize(factory);
		final AtomicReference<ServletContext> servletContextReference = new AtomicReference<ServletContext>();
		EmbeddedServletContainer container = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {

					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						servletContextReference.set(servletContext);
					}

				});
		try {
			container.start();
			assertThat(servletContextReference.get().getEffectiveSessionTrackingModes())
					.isEqualTo(EnumSet.of(SessionTrackingMode.SSL));
		}
		finally {
			container.stop();
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
		assertThat(this.properties.getJetty().getMaxHttpPostSize()).isEqualTo(9999);
		assertThat(this.properties.getTomcat().getMaxHttpPostSize()).isEqualTo(9999);
		assertThat(this.properties.getUndertow().getMaxHttpPostSize()).isEqualTo(9999);
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
	public void defaultTomcatBackgroundProcessorDelay() throws Exception {
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(factory);
		EmbeddedServletContainer container = factory.getEmbeddedServletContainer();
		assertThat(((TomcatEmbeddedServletContainer) container).getTomcat().getEngine()
				.getBackgroundProcessorDelay()).isEqualTo(10);
		container.stop();
	}

	@Test
	public void customTomcatBackgroundProcessorDelay() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.background-processor-delay", "5");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(factory);
		EmbeddedServletContainer container = factory.getEmbeddedServletContainer();
		assertThat(((TomcatEmbeddedServletContainer) container).getTomcat().getEngine()
				.getBackgroundProcessorDelay()).isEqualTo(5);
		container.stop();
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
				+ "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" //
				+ "0:0:0:0:0:0:0:1|::1";
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
	public void customTomcatAcceptCount() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.accept-count", "10");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory(
				0);
		this.properties.customize(container);
		TomcatEmbeddedServletContainer embeddedContainer = (TomcatEmbeddedServletContainer) container
				.getEmbeddedServletContainer();
		embeddedContainer.start();
		try {
			assertThat(((AbstractProtocol<?>) embeddedContainer.getTomcat().getConnector()
					.getProtocolHandler()).getAcceptCount()).isEqualTo(10);
		}
		finally {
			embeddedContainer.stop();
		}
	}

	@Test
	public void customTomcatMaxConnections() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.max-connections", "5");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory(
				0);
		this.properties.customize(container);
		TomcatEmbeddedServletContainer embeddedContainer = (TomcatEmbeddedServletContainer) container
				.getEmbeddedServletContainer();
		embeddedContainer.start();
		try {
			assertThat(((AbstractProtocol<?>) embeddedContainer.getTomcat().getConnector()
					.getProtocolHandler()).getMaxConnections()).isEqualTo(5);
		}
		finally {
			embeddedContainer.stop();
		}
	}

	@Test
	public void customTomcatMaxHttpPostSize() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.max-http-post-size", "10000");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory(
				0);
		this.properties.customize(container);
		TomcatEmbeddedServletContainer embeddedContainer = (TomcatEmbeddedServletContainer) container
				.getEmbeddedServletContainer();
		embeddedContainer.start();
		try {
			assertThat(embeddedContainer.getTomcat().getConnector().getMaxPostSize())
					.isEqualTo(10000);
		}
		finally {
			embeddedContainer.stop();
		}
	}

	@Test
	public void customTomcatDisableMaxHttpPostSize() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.max-http-post-size", "-1");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory(
				0);
		this.properties.customize(container);
		TomcatEmbeddedServletContainer embeddedContainer = (TomcatEmbeddedServletContainer) container
				.getEmbeddedServletContainer();
		embeddedContainer.start();
		try {
			assertThat(embeddedContainer.getTomcat().getConnector().getMaxPostSize())
					.isEqualTo(-1);
		}
		finally {
			embeddedContainer.stop();
		}
	}

	@Test
	@Deprecated
	public void customTomcatMaxHttpPostSizeWithDeprecatedProperty() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.max-http-post-size", "2000");
		bindProperties(map);
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory(
				0);
		this.properties.customize(container);
		TomcatEmbeddedServletContainer embeddedContainer = (TomcatEmbeddedServletContainer) container
				.getEmbeddedServletContainer();
		embeddedContainer.start();
		try {
			assertThat(embeddedContainer.getTomcat().getConnector().getMaxPostSize())
					.isEqualTo(2000);
		}
		finally {
			embeddedContainer.stop();
		}
	}

	@Test
	public void customizeUndertowAccessLog() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.undertow.accesslog.enabled", "true");
		map.put("server.undertow.accesslog.pattern", "foo");
		map.put("server.undertow.accesslog.prefix", "test_log");
		map.put("server.undertow.accesslog.suffix", "txt");
		map.put("server.undertow.accesslog.dir", "test-logs");
		map.put("server.undertow.accesslog.rotate", "false");
		bindProperties(map);
		UndertowEmbeddedServletContainerFactory container = spy(
				new UndertowEmbeddedServletContainerFactory());
		this.properties.getUndertow().customizeUndertow(this.properties, container);
		verify(container).setAccessLogEnabled(true);
		verify(container).setAccessLogPattern("foo");
		verify(container).setAccessLogPrefix("test_log");
		verify(container).setAccessLogSuffix("txt");
		verify(container).setAccessLogDirectory(new File("test-logs"));
		verify(container).setAccessLogRotate(false);
	}

	@Test
	public void customTomcatTldSkip() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.additional-tld-skip-patterns", "foo.jar,bar.jar");
		bindProperties(map);
		testCustomTomcatTldSkip("foo.jar", "bar.jar");
	}

	@Test
	public void customTomcatTldSkipAsList() {
		Map<String, String> map = new HashMap<String, String>();
		map.put("server.tomcat.additional-tld-skip-patterns[0]", "biz.jar");
		map.put("server.tomcat.additional-tld-skip-patterns[1]", "bah.jar");
		bindProperties(map);
		testCustomTomcatTldSkip("biz.jar", "bah.jar");
	}

	private void testCustomTomcatTldSkip(String... expectedJars) {
		TomcatEmbeddedServletContainerFactory container = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(container);
		assertThat(container.getTldSkipPatterns()).contains(expectedJars);
		assertThat(container.getTldSkipPatterns()).contains("junit-*.jar",
				"spring-boot-*.jar");
	}

	@Test
	public void customTomcatHttpOnlyCookie() throws Exception {
		this.properties.getSession().getCookie().setHttpOnly(false);
		TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
		this.properties.customize(factory);
		EmbeddedServletContainer container = factory.getEmbeddedServletContainer();
		Tomcat tomcat = ((TomcatEmbeddedServletContainer) container).getTomcat();
		StandardContext context = (StandardContext) tomcat.getHost().findChildren()[0];
		assertThat(context.getUseHttpOnly()).isFalse();
		container.stop();
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

	@Test
	public void tomcatAcceptCountMatchesProtocolDefault() throws Exception {
		assertThat(this.properties.getTomcat().getAcceptCount())
				.isEqualTo(getDefaultProtocol().getAcceptCount());
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
		assertThat(this.properties.getTomcat().getMaxHttpPostSize())
				.isEqualTo(getDefaultConnector().getMaxPostSize());
	}

	@Test
	public void tomcatBackgroundProcessorDelayMatchesEngineDefault() {
		assertThat(this.properties.getTomcat().getBackgroundProcessorDelay())
				.isEqualTo(new StandardEngine().getBackgroundProcessorDelay());
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
		JettyEmbeddedServletContainerFactory jettyFactory = new JettyEmbeddedServletContainerFactory(
				0);
		JettyEmbeddedServletContainer jetty = (JettyEmbeddedServletContainer) jettyFactory
				.getEmbeddedServletContainer(new ServletContextInitializer() {

					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						servletContext.addServlet("formPost", new HttpServlet() {

							@Override
							protected void doPost(HttpServletRequest req,
									HttpServletResponse resp)
									throws ServletException, IOException {
								req.getParameterMap();
							}

						}).addMapping("/form");
					}

				});
		jetty.start();
		org.eclipse.jetty.server.Connector connector = jetty.getServer()
				.getConnectors()[0];
		final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
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
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
			StringBuilder data = new StringBuilder();
			for (int i = 0; i < 250000; i++) {
				data.append("a");
			}
			body.add("data", data.toString());
			HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<MultiValueMap<String, Object>>(
					body, headers);
			template.postForEntity(
					URI.create("http://localhost:" + jetty.getPort() + "/form"), entity,
					Void.class);
			assertThat(failure.get()).isNotNull();
			String message = failure.get().getCause().getMessage();
			int defaultMaxPostSize = Integer
					.valueOf(message.substring(message.lastIndexOf(' ')).trim());
			assertThat(this.properties.getJetty().getMaxHttpPostSize())
					.isEqualTo(defaultMaxPostSize);
		}
		finally {
			jetty.stop();
		}
	}

	@Test
	public void undertowMaxHttpPostSizeMatchesDefault() {
		assertThat(this.properties.getUndertow().getMaxHttpPostSize())
				.isEqualTo(UndertowOptions.DEFAULT_MAX_ENTITY_SIZE);
	}

	private Connector getDefaultConnector() throws Exception {
		return new Connector(TomcatEmbeddedServletContainerFactory.DEFAULT_PROTOCOL);
	}

	private AbstractProtocol<?> getDefaultProtocol() throws Exception {
		return (AbstractProtocol<?>) Class
				.forName(TomcatEmbeddedServletContainerFactory.DEFAULT_PROTOCOL)
				.newInstance();
	}

	private void bindProperties(Map<String, String> map) {
		new RelaxedDataBinder(this.properties, "server")
				.bind(new MutablePropertyValues(map));
	}

}
