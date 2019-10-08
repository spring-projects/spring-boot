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

package org.springframework.boot.autoconfigure.web.embedded;

import java.util.Locale;
import java.util.function.Consumer;

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatWebServerFactoryCustomizer}
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author Rob Tompkins
 * @author Artsiom Yudovin
 * @author Stephane Nicoll
 * @author Andrew McGhie
 */
class TomcatWebServerFactoryCustomizerTests {

	private MockEnvironment environment;

	private ServerProperties serverProperties;

	private TomcatWebServerFactoryCustomizer customizer;

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.serverProperties = new ServerProperties();
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new TomcatWebServerFactoryCustomizer(this.environment, this.serverProperties);
	}

	@Test
	void defaultsAreConsistent() {
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getMaxSwallowSize())
								.isEqualTo(this.serverProperties.getTomcat().getMaxSwallowSize().toBytes()));
	}

	@Test
	void customAcceptCount() {
		bind("server.tomcat.accept-count=10");
		customizeAndRunServer((server) -> assertThat(
				((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getAcceptCount())
						.isEqualTo(10));
	}

	@Test
	void customProcessorCache() {
		bind("server.tomcat.processor-cache=100");
		customizeAndRunServer((server) -> assertThat(
				((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getProcessorCache())
						.isEqualTo(100));
	}

	@Test
	void unlimitedProcessorCache() {
		bind("server.tomcat.processor-cache=-1");
		customizeAndRunServer((server) -> assertThat(
				((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getProcessorCache())
						.isEqualTo(-1));
	}

	@Test
	void customBackgroundProcessorDelay() {
		bind("server.tomcat.background-processor-delay=5");
		TomcatWebServer server = customizeAndGetServer();
		assertThat(server.getTomcat().getEngine().getBackgroundProcessorDelay()).isEqualTo(5);
	}

	@Test
	void customDisableMaxHttpPostSize() {
		bind("server.tomcat.max-http-post-size=-1");
		customizeAndRunServer((server) -> assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(-1));
	}

	@Test
	void customMaxConnections() {
		bind("server.tomcat.max-connections=5");
		customizeAndRunServer((server) -> assertThat(
				((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getMaxConnections())
						.isEqualTo(5));
	}

	@Test
	void customMaxHttpPostSize() {
		bind("server.tomcat.max-http-post-size=10000");
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(10000));
	}

	@Test
	void customMaxHttpHeaderSize() {
		bind("server.max-http-header-size=1KB");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getMaxHttpHeaderSize()).isEqualTo(DataSize.ofKilobytes(1).toBytes()));
	}

	@Test
	void customMaxHttpHeaderSizeIgnoredIfNegative() {
		bind("server.max-http-header-size=-1");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getMaxHttpHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxHttpHeaderSizeIgnoredIfZero() {
		bind("server.max-http-header-size=0");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getMaxHttpHeaderSize()).isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxSwallowSize() {
		bind("server.tomcat.max-swallow-size=10MB");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getMaxSwallowSize()).isEqualTo(DataSize.ofMegabytes(10).toBytes()));
	}

	@Test
	void customRemoteIpValve() {
		bind("server.tomcat.remote-ip-header=x-my-remote-ip-header",
				"server.tomcat.protocol-header=x-my-protocol-header", "server.tomcat.internal-proxies=192.168.0.1",
				"server.tomcat.port-header=x-my-forward-port", "server.tomcat.protocol-header-https-value=On");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
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
	void customStaticResourceAllowCaching() {
		bind("server.tomcat.resource.allow-caching=false");
		customizeAndRunServer((server) -> {
			Tomcat tomcat = server.getTomcat();
			Context context = (Context) tomcat.getHost().findChildren()[0];
			assertThat(context.getResources().isCachingAllowed()).isFalse();
		});
	}

	@Test
	void customStaticResourceCacheTtl() {
		bind("server.tomcat.resource.cache-ttl=10000");
		customizeAndRunServer((server) -> {
			Tomcat tomcat = server.getTomcat();
			Context context = (Context) tomcat.getHost().findChildren()[0];
			assertThat(context.getResources().getCacheTtl()).isEqualTo(10000L);
		});
	}

	@Test
	void customRelaxedPathChars() {
		bind("server.tomcat.relaxed-path-chars=|,^");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getRelaxedPathChars()).isEqualTo("|^"));
	}

	@Test
	void customRelaxedQueryChars() {
		bind("server.tomcat.relaxed-query-chars=^  ,  | ");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
						.getRelaxedQueryChars()).isEqualTo("^|"));
	}

	@Test
	void deduceUseForwardHeaders() {
		this.environment.setProperty("DYNO", "-");
		testRemoteIpValveConfigured();
	}

	@Test
	void defaultRemoteIpValve() {
		// Since 1.1.7 you need to specify at least the protocol
		bind("server.tomcat.protocol-header=X-Forwarded-Proto", "server.tomcat.remote-ip-header=X-Forwarded-For");
		testRemoteIpValveConfigured();
	}

	@Test
	void setUseForwardHeaders() {
		// Since 1.3.0 no need to explicitly set header names if use-forward-header=true
		this.serverProperties.setUseForwardHeaders(true);
		testRemoteIpValveConfigured();
	}

	private void testRemoteIpValveConfigured() {
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(factory.getEngineValves()).hasSize(1);
		Valve valve = factory.getEngineValves().iterator().next();
		assertThat(valve).isInstanceOf(RemoteIpValve.class);
		RemoteIpValve remoteIpValve = (RemoteIpValve) valve;
		assertThat(remoteIpValve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
		assertThat(remoteIpValve.getProtocolHeaderHttpsValue()).isEqualTo("https");
		assertThat(remoteIpValve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
		assertThat(remoteIpValve.getHostHeader()).isEqualTo("X-Forwarded-Host");
		assertThat(remoteIpValve.getPortHeader()).isEqualTo("X-Forwarded-Port");
		String expectedInternalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
				+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
				+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
				+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
				+ "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
				+ "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" //
				+ "0:0:0:0:0:0:0:1|::1";
		assertThat(remoteIpValve.getInternalProxies()).isEqualTo(expectedInternalProxies);
	}

	@Test
	void defaultBackgroundProcessorDelay() {
		TomcatWebServer server = customizeAndGetServer();
		assertThat(server.getTomcat().getEngine().getBackgroundProcessorDelay()).isEqualTo(10);
	}

	@Test
	void disableRemoteIpValve() {
		bind("server.tomcat.remote-ip-header=", "server.tomcat.protocol-header=");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(factory.getEngineValves()).isEmpty();
	}

	@Test
	void errorReportValveIsConfiguredToNotReportStackTraces() {
		TomcatWebServer server = customizeAndGetServer();
		Valve[] valves = server.getTomcat().getHost().getPipeline().getValves();
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
	void testCustomizeMinSpareThreads() {
		bind("server.tomcat.min-spare-threads=10");
		assertThat(this.serverProperties.getTomcat().getMinSpareThreads()).isEqualTo(10);
	}

	@Test
	void accessLogBufferingCanBeDisabled() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.buffered=false");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isBuffered()).isFalse();
	}

	@Test
	void accessLogCanBeEnabled() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(factory.getEngineValves()).hasSize(1);
		assertThat(factory.getEngineValves()).first().isInstanceOf(AccessLogValve.class);
	}

	@Test
	void accessLogFileDateFormatByDefault() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getFileDateFormat())
				.isEqualTo(".yyyy-MM-dd");
	}

	@Test
	void accessLogFileDateFormatCanBeRedefined() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.file-date-format=yyyy-MM-dd.HH");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getFileDateFormat())
				.isEqualTo("yyyy-MM-dd.HH");
	}

	@Test
	void accessLogIsBufferedByDefault() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isBuffered()).isTrue();
	}

	@Test
	void accessLogIsDisabledByDefault() {
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(factory.getEngineValves()).isEmpty();
	}

	@Test
	void accessLogMaxDaysDefault() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getMaxDays())
				.isEqualTo(this.serverProperties.getTomcat().getAccesslog().getMaxDays());
	}

	@Test
	void accessLogConditionCanBeSpecified() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.conditionIf=foo",
				"server.tomcat.accesslog.conditionUnless=bar");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getConditionIf()).isEqualTo("foo");
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getConditionUnless())
				.isEqualTo("bar");
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getCondition())
				.describedAs("value of condition should equal conditionUnless - provided for backwards compatibility")
				.isEqualTo("bar");
	}

	@Test
	void accessLogEncodingIsNullWhenNotSpecified() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getEncoding()).isNull();
	}

	@Test
	void accessLogEncodingCanBeSpecified() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.encoding=UTF-8");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getEncoding()).isEqualTo("UTF-8");
	}

	@Test
	void accessLogWithDefaultLocale() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getLocale())
				.isEqualTo(Locale.getDefault().toString());
	}

	@Test
	void accessLogLocaleCanBeSpecified() {
		String locale = "en_AU".equals(Locale.getDefault().toString()) ? "en_US" : "en_AU";
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.locale=" + locale);
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getLocale()).isEqualTo(locale);
	}

	@Test
	void accessLogCheckExistsDefault() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isCheckExists()).isFalse();
	}

	@Test
	void accessLogCheckExistsSpecified() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.check-exists=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).isCheckExists()).isTrue();
	}

	@Test
	void accessLogMaxDaysCanBeRedefined() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.max-days=20");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getMaxDays()).isEqualTo(20);
	}

	@Test
	void accessLogDoesNotUseIpv6CanonicalFormatByDefault() {
		bind("server.tomcat.accesslog.enabled=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getIpv6Canonical()).isFalse();
	}

	@Test
	void accessLogwithIpv6CanonicalSet() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.ipv6-canonical=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getIpv6Canonical()).isTrue();
	}

	@Test
	void ajpConnectorCanBeCustomized() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		factory.setProtocol("AJP/1.3");
		this.customizer.customize(factory);
		WebServer server = factory.getWebServer();
		server.start();
		server.stop();
	}

	private void bind(String... inlinedProperties) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
		new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
				Bindable.ofInstance(this.serverProperties));
	}

	private void customizeAndRunServer(Consumer<TomcatWebServer> consumer) {
		TomcatWebServer server = customizeAndGetServer();
		server.start();
		try {
			consumer.accept(server);
		}
		finally {
			server.stop();
		}
	}

	private TomcatWebServer customizeAndGetServer() {
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		return (TomcatWebServer) factory.getWebServer();
	}

	private TomcatServletWebServerFactory customizeAndGetFactory() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		this.customizer.customize(factory);
		return factory;
	}

}
