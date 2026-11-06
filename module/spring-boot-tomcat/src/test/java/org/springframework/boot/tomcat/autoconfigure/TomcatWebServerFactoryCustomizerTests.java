/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ajp.AbstractAjpProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.tomcat.RemoteIp;
import org.springframework.boot.tomcat.RemoteIp.Header;
import org.springframework.boot.tomcat.RemoteIp.Proxies;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
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
 * @author Rafiullah Hamedy
 * @author Victor Mandujano
 * @author Parviz Rozikov
 * @author Moritz Halbritter
 */
@DirtiesUrlFactories
class TomcatWebServerFactoryCustomizerTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final ServerProperties serverProperties = new ServerProperties();

	private final TomcatServerProperties tomcatProperties = new TomcatServerProperties();

	private final WebProperties webProperties = new WebProperties();

	private TomcatWebServerFactoryCustomizer customizer;

	@BeforeEach
	void setup() {
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new TomcatWebServerFactoryCustomizer(this.environment, this.serverProperties,
				this.tomcatProperties, this.webProperties);
	}

	@Test
	void defaultsAreConsistent() {
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxSwallowSize())
			.isEqualTo(this.tomcatProperties.getMaxSwallowSize().toBytes()));
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
	void customKeepAliveTimeout() {
		bind("server.tomcat.keep-alive-timeout=30ms");
		customizeAndRunServer((server) -> assertThat(
				((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getKeepAliveTimeout())
			.isEqualTo(30));
	}

	@Test
	void defaultKeepAliveTimeoutWithHttp2() {
		bind("server.http2.enabled=true");
		customizeAndRunServer((server) -> assertThat(
				((Http2Protocol) server.getTomcat().getConnector().findUpgradeProtocols()[0]).getKeepAliveTimeout())
			.isEqualTo(20000L));
	}

	@Test
	void customKeepAliveTimeoutWithHttp2() {
		bind("server.tomcat.keep-alive-timeout=30s", "server.http2.enabled=true");
		customizeAndRunServer((server) -> assertThat(
				((Http2Protocol) server.getTomcat().getConnector().findUpgradeProtocols()[0]).getKeepAliveTimeout())
			.isEqualTo(30000L));
	}

	@Test
	void customMaxKeepAliveRequests() {
		bind("server.tomcat.max-keep-alive-requests=-1");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxKeepAliveRequests())
			.isEqualTo(-1));
	}

	@Test
	void defaultMaxKeepAliveRequests() {
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxKeepAliveRequests())
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
	void customDisableMaxHttpFormPostSize() {
		bind("server.tomcat.max-http-form-post-size=-1");
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
	void customMaxHttpFormPostSize() {
		bind("server.tomcat.max-http-form-post-size=10000");
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxPostSize()).isEqualTo(10000));
	}

	@Test
	void defaultMaxPartCount() {
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxPartCount()).isEqualTo(50));
	}

	@Test
	void customMaxPartCount() {
		bind("server.tomcat.max-part-count=5");
		customizeAndRunServer((server) -> assertThat(server.getTomcat().getConnector().getMaxPartCount()).isEqualTo(5));
	}

	@Test
	void defaultMaxPartHeaderSize() {
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxPartHeaderSize()).isEqualTo(512));
	}

	@Test
	void customMaxPartHeaderSize() {
		bind("server.tomcat.max-part-header-size=4KB");
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxPartHeaderSize()).isEqualTo(4096));
	}

	@Test
	void defaultMaxHttpRequestHeaderSize() {
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpRequestHeaderSize())
			.isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxHttpRequestHeaderSize() {
		bind("server.max-http-request-header-size=10MB");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpRequestHeaderSize())
			.isEqualTo(DataSize.ofMegabytes(10).toBytes()));
	}

	@Test
	void customMaxParameterCount() {
		bind("server.tomcat.max-parameter-count=100");
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxParameterCount()).isEqualTo(100));
	}

	@Test
	void defaultMaxLocaleCount() {
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxLocaleCount()).isEqualTo(10));
	}

	@Test
	void customMaxLocaleCount() {
		bind("server.tomcat.max-locale-count=-1");
		customizeAndRunServer(
				(server) -> assertThat(server.getTomcat().getConnector().getMaxLocaleCount()).isEqualTo(-1));
	}

	@Test
	void customMaxRequestHttpHeaderSizeIgnoredIfNegative() {
		bind("server.max-http-request-header-size=-1");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpRequestHeaderSize())
			.isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxRequestHttpHeaderSizeIgnoredIfZero() {
		bind("server.max-http-request-header-size=0");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpRequestHeaderSize())
			.isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void defaultMaxHttpResponseHeaderSize() {
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpResponseHeaderSize())
			.isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxHttpResponseHeaderSize() {
		bind("server.tomcat.max-http-response-header-size=10MB");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpResponseHeaderSize())
			.isEqualTo(DataSize.ofMegabytes(10).toBytes()));
	}

	@Test
	void customMaxResponseHttpHeaderSizeIgnoredIfNegative() {
		bind("server.tomcat.max-http-response-header-size=-1");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpResponseHeaderSize())
			.isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxResponseHttpHeaderSizeIgnoredIfZero() {
		bind("server.tomcat.max-http-response-header-size=0");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxHttpResponseHeaderSize())
			.isEqualTo(DataSize.ofKilobytes(8).toBytes()));
	}

	@Test
	void customMaxSwallowSize() {
		bind("server.tomcat.max-swallow-size=10MB");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getMaxSwallowSize())
			.isEqualTo(DataSize.ofMegabytes(10).toBytes()));
	}

	@Test
	void remoteIpWhenCustomized() {
		bind("server.tomcat.remoteip.remote-ip-header=x-my-remote-ip-header",
				"server.tomcat.remoteip.protocol-header=x-my-protocol-header",
				"server.tomcat.remoteip.internal-proxies=192.168.0.1",
				"server.tomcat.remoteip.host-header=x-my-forward-host",
				"server.tomcat.remoteip.port-header=x-my-forward-port",
				"server.tomcat.remoteip.protocol-header-https-value=On",
				"server.tomcat.remoteip.trusted-proxies=proxy1|proxy2");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		Header expectedHeader = new Header("x-my-forward-host", "x-my-forward-port", "x-my-remote-ip-header",
				"x-my-protocol-header", "On");
		Proxies expectedProxies = new Proxies("192.168.0.1", "proxy1|proxy2");
		RemoteIp expected = new RemoteIp(expectedHeader, expectedProxies, RemoteIp.Usage.ALWAYS);
		assertThat(factory.getRemoteIp()).isEqualTo(expected);
	}

	@Test
	void remoteIpWhenCustomizedProxies() {
		bind("server.tomcat.remoteip.internal-proxies=192.168.0.1",
				"server.tomcat.remoteip.trusted-proxies=proxy1|proxy2");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		Header expectedHeader = new Header("X-Forwarded-Host", "X-Forwarded-Port", null, null, "https");
		Proxies expectedProxies = new Proxies("192.168.0.1", "proxy1|proxy2");
		RemoteIp expected = new RemoteIp(expectedHeader, expectedProxies, RemoteIp.Usage.FOR_FORWARDED_REQUEST_HEADERS);
		assertThat(factory.getRemoteIp()).isEqualTo(expected);
	}

	@Test
	void remoteIpWhenOnlyProtocolHeader() {
		bind("server.tomcat.remoteip.protocol-header=X-Forwarded-Proto");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		TomcatServerProperties defaultProperties = new TomcatServerProperties();
		Header expectedHeader = new Header("X-Forwarded-Host", "X-Forwarded-Port", null, "X-Forwarded-Proto", "https");
		Proxies expectedProxies = new Proxies(defaultProperties.getRemoteip().getInternalProxies(),
				defaultProperties.getRemoteip().getTrustedProxies());
		RemoteIp expected = new RemoteIp(expectedHeader, expectedProxies, RemoteIp.Usage.ALWAYS);
		assertThat(factory.getRemoteIp()).isEqualTo(expected);
	}

	@Test
	void remoteIpWhenOnlyRemoteIp() {
		bind("server.tomcat.remoteip.remote-ip-header=X-Forwarded-For");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		TomcatServerProperties defaultProperties = new TomcatServerProperties();
		Header expectedHeader = new Header("X-Forwarded-Host", "X-Forwarded-Port", "X-Forwarded-For", null, "https");
		Proxies expectedProxies = new Proxies(defaultProperties.getRemoteip().getInternalProxies(),
				defaultProperties.getRemoteip().getTrustedProxies());
		RemoteIp expected = new RemoteIp(expectedHeader, expectedProxies, RemoteIp.Usage.ALWAYS);
		assertThat(factory.getRemoteIp()).isEqualTo(expected);
	}

	@Test
	void resourceCacheMatchesDefault() {
		TomcatServerProperties properties = new TomcatServerProperties();
		customizeAndRunServer((server) -> {
			Tomcat tomcat = server.getTomcat();
			Context context = (Context) tomcat.getHost().findChildren()[0];
			assertThat(properties.getResource().isAllowCaching()).isEqualTo(context.getResources().isCachingAllowed());
			assertThat(properties.getResource().getCacheMaxSize())
				.isEqualTo(DataSize.ofKilobytes(context.getResources().getCacheMaxSize()));
			assertThat(properties.getResource().getCacheTtl())
				.isEqualTo(Duration.ofMillis(context.getResources().getCacheTtl()));
		});
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
	void customStaticResourceCacheMaxSize() {
		bind("server.tomcat.resource.cache-max-size=4MB");
		customizeAndRunServer((server) -> {
			Tomcat tomcat = server.getTomcat();
			Context context = (Context) tomcat.getHost().findChildren()[0];
			assertThat(context.getResources().getCacheMaxSize()).isEqualTo(4096L);
		});
	}

	@Test
	void customStaticResourceCacheTtl() {
		bind("server.tomcat.resource.cache-ttl=10s");
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
					.getRelaxedPathChars())
			.isEqualTo("|^"));
	}

	@Test
	void customRelaxedQueryChars() {
		bind("server.tomcat.relaxed-query-chars=^  ,  | ");
		customizeAndRunServer((server) -> assertThat(
				((AbstractHttp11Protocol<?>) server.getTomcat().getConnector().getProtocolHandler())
					.getRelaxedQueryChars())
			.isEqualTo("^|"));
	}

	@Test
	void defaultBackgroundProcessorDelay() {
		TomcatWebServer server = customizeAndGetServer();
		assertThat(server.getTomcat().getEngine().getBackgroundProcessorDelay()).isEqualTo(10);
	}

	@Test
	void disableRemoteIpValve() {
		bind("server.tomcat.remoteip.remote-ip-header=", "server.tomcat.remoteip.protocol-header=");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(factory.getEngineValves()).isEmpty();
	}

	@Test
	void errorReportValveIsConfiguredToNotReportStackTraces() {
		TomcatWebServer server = customizeAndGetServer();
		Valve[] valves = server.getTomcat().getHost().getPipeline().getValves();
		assertThat(valves).hasAtLeastOneElementOfType(ErrorReportValve.class);
		for (Valve valve : valves) {
			if (valve instanceof ErrorReportValve errorReportValve) {
				assertThat(errorReportValve.isShowReport()).isFalse();
				assertThat(errorReportValve.isShowServerInfo()).isFalse();
			}
		}
	}

	@Test
	void testCustomizeMinSpareThreads() {
		bind("server.tomcat.threads.min-spare=10");
		assertThat(this.tomcatProperties.getThreads().getMinSpare()).isEqualTo(10);
	}

	@Test
	void customConnectionTimeout() {
		bind("server.tomcat.connection-timeout=30s");
		customizeAndRunServer((server) -> assertThat(
				((AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler()).getConnectionTimeout())
			.isEqualTo(30000));
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
			.isEqualTo(this.tomcatProperties.getAccesslog().getMaxDays());
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
	void accessLogWithIpv6CanonicalSet() {
		bind("server.tomcat.accesslog.enabled=true", "server.tomcat.accesslog.ipv6-canonical=true");
		TomcatServletWebServerFactory factory = customizeAndGetFactory();
		assertThat(((AccessLogValve) factory.getEngineValves().iterator().next()).getIpv6Canonical()).isTrue();
	}

	@Test
	void ajpConnectorCanBeCustomized() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		factory.setProtocol("AJP/1.3");
		factory.addConnectorCustomizers(
				(connector) -> ((AbstractAjpProtocol<?>) connector.getProtocolHandler()).setSecretRequired(false));
		this.customizer.customize(factory);
		WebServer server = factory.getWebServer();
		server.start();
		server.stop();
	}

	@Test
	void configureExecutor() {
		bind("server.tomcat.threads.max=10", "server.tomcat.threads.min-spare=2",
				"server.tomcat.threads.max-queue-capacity=20");
		customizeAndRunServer((server) -> {
			AbstractProtocol<?> protocol = (AbstractProtocol<?>) server.getTomcat().getConnector().getProtocolHandler();
			assertThat(protocol.getMaxThreads()).isEqualTo(10);
			assertThat(protocol.getMinSpareThreads()).isEqualTo(2);
			assertThat(protocol.getMaxQueueSize()).isEqualTo(20);
		});
	}

	@Test
	void enableMBeanRegistry() {
		bind("server.tomcat.mbeanregistry.enabled=true");
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		assertThat(factory.isDisableMBeanRegistry()).isTrue();
		this.customizer.customize(factory);
		assertThat(factory.isDisableMBeanRegistry()).isFalse();
	}

	private void bind(String... inlinedProperties) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
		Binder binder = new Binder(ConfigurationPropertySources.get(this.environment));
		binder.bind("server", Bindable.ofInstance(this.serverProperties));
		binder.bind("server.tomcat", Bindable.ofInstance(this.tomcatProperties));
	}

	private void customizeAndRunServer(@Nullable Consumer<TomcatWebServer> consumer) {
		TomcatWebServer server = customizeAndGetServer();
		server.start();
		try {
			if (consumer != null) {
				consumer.accept(server);
			}
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
		factory.setHttp2(this.serverProperties.getHttp2());
		this.customizer.customize(factory);
		return factory;
	}

}
