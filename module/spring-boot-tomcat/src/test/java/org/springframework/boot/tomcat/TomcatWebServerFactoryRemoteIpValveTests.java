/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import java.util.Arrays;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.RemoteIpValve;
import org.junit.jupiter.api.Test;

import org.springframework.boot.tomcat.RemoteIp.Header;
import org.springframework.boot.tomcat.RemoteIp.Proxies;
import org.springframework.boot.tomcat.RemoteIp.Usage;
import org.springframework.boot.web.server.ForwardedRequestHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for nested testing of {@link TomcatWebServerFactory} {@link RemoteIpValve}.
 *
 * @param <F> the factory type
 * @author Phillip Webb
 */
public abstract class TomcatWebServerFactoryRemoteIpValveTests<F extends TomcatWebServerFactory> {

	private static final String TOMCAT_DEFAULT_INTERNAL_PROXIES = new RemoteIpValve().getInternalProxies();

	@Test
	void whenNoForwardedRequestHeadersOrRemoteIp() {
		F factory = getFactory();
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves).filter(RemoteIpValve.class::isInstance).findFirst()).isEmpty();
	}

	@Test
	void whenNoForwardedRequestHeadersAndRemoteIpUsageForForwardRequestHeaders() {
		F factory = getFactory();
		factory.setRemoteIp(new RemoteIp(null, null, RemoteIp.Usage.FOR_FORWARDED_REQUEST_HEADERS));
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves).filter(RemoteIpValve.class::isInstance).findFirst()).isEmpty();
	}

	@Test
	void whenNoForwardedRequestHeadersAndFullyConfiguredAlwaysRemoteIp() {
		F factory = getFactory();
		factory.setRemoteIp(new RemoteIp(new Header("x-my-forward-host", "x-my-forward-port", "x-my-remote-ip-header",
				"x-my-protocol-header", "On"), new Proxies("192.168.0.1", "proxy1|proxy2")));
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves)
			.filter(RemoteIpValve.class::isInstance)
			.map(RemoteIpValve.class::cast)
			.findFirst()
			.get()).satisfies((valve) -> {
				assertThat(valve.getHostHeader()).isEqualTo("x-my-forward-host");
				assertThat(valve.getPortHeader()).isEqualTo("x-my-forward-port");
				assertThat(valve.getRemoteIpHeader()).isEqualTo("x-my-remote-ip-header");
				assertThat(valve.getProtocolHeader()).isEqualTo("x-my-protocol-header");
				assertThat(valve.getProtocolHeaderHttpsValue()).isEqualTo("On");
				assertThat(valve.getInternalProxies()).isEqualTo("192.168.0.1");
				assertThat(valve.getTrustedProxies()).isEqualTo("proxy1|proxy2");
			});
	}

	@Test
	void whenNoForwardedRequestHeadersAndDefaultsRemoteIp() {
		F factory = getFactory();
		factory.setRemoteIp(RemoteIp.DEFAULTS);
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves)
			.filter(RemoteIpValve.class::isInstance)
			.map(RemoteIpValve.class::cast)
			.findFirst()
			.get()).satisfies((valve) -> {
				assertThat(valve.getHostHeader()).isNull();
				assertThat(valve.getPortHeader()).isNull();
				assertThat(valve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
				assertThat(valve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
				assertThat(valve.getProtocolHeaderHttpsValue()).isEqualTo("https");
				assertThat(valve.getInternalProxies()).isEqualTo(TOMCAT_DEFAULT_INTERNAL_PROXIES);
				assertThat(valve.getTrustedProxies()).isNull();
			});
	}

	@Test
	void whenForwardedRequestHeadersAndNoRemoteIp() {
		F factory = getFactory();
		factory.setForwardedRequestHeaders(ForwardedRequestHeaders.X_FORWARDED_WITHOUT_PREFIX);
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves)
			.filter(RemoteIpValve.class::isInstance)
			.map(RemoteIpValve.class::cast)
			.findFirst()
			.get()).satisfies((valve) -> {
				assertThat(valve.getHostHeader()).isEqualTo("X-Forwarded-Host");
				assertThat(valve.getPortHeader()).isEqualTo("X-Forwarded-Port");
				assertThat(valve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
				assertThat(valve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
				assertThat(valve.getProtocolHeaderHttpsValue()).isEqualTo("https");
				assertThat(valve.getInternalProxies()).isEqualTo(TOMCAT_DEFAULT_INTERNAL_PROXIES);
				assertThat(valve.getTrustedProxies()).isNull();
			});
	}

	@Test
	void whenForwardedRequestHeadersAndNoRemoteIpWithHostAndPortOverride() {
		F factory = getFactory();
		factory.setForwardedRequestHeaders(ForwardedRequestHeaders.X_FORWARDED_WITHOUT_PREFIX);
		factory.setRemoteIp(new RemoteIp(new Header("x-my-forward-host", "x-my-forward-port", null, null), null,
				Usage.FOR_FORWARDED_REQUEST_HEADERS));
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves)
			.filter(RemoteIpValve.class::isInstance)
			.map(RemoteIpValve.class::cast)
			.findFirst()
			.get()).satisfies((valve) -> {
				assertThat(valve.getHostHeader()).isEqualTo("x-my-forward-host");
				assertThat(valve.getPortHeader()).isEqualTo("x-my-forward-port");
				assertThat(valve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
				assertThat(valve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
				assertThat(valve.getProtocolHeaderHttpsValue()).isEqualTo("https");
				assertThat(valve.getInternalProxies()).isEqualTo(TOMCAT_DEFAULT_INTERNAL_PROXIES);
				assertThat(valve.getTrustedProxies()).isNull();
			});
	}

	@Test
	void whenForwardedRequestHeadersAndRemoteIp() {
		F factory = getFactory();
		factory.setForwardedRequestHeaders(ForwardedRequestHeaders.X_FORWARDED_WITHOUT_PREFIX);
		factory.setRemoteIp(new RemoteIp(null, new Proxies("192.168.0.1", "proxy1|proxy2"),
				RemoteIp.Usage.FOR_FORWARDED_REQUEST_HEADERS));
		TomcatWebServer webServer = getWebServer(factory);
		Valve[] valves = webServer.getTomcat().getEngine().getPipeline().getValves();
		assertThat(Arrays.stream(valves)
			.filter(RemoteIpValve.class::isInstance)
			.map(RemoteIpValve.class::cast)
			.findFirst()
			.get()).satisfies((valve) -> {
				assertThat(valve.getHostHeader()).isEqualTo("X-Forwarded-Host");
				assertThat(valve.getPortHeader()).isEqualTo("X-Forwarded-Port");
				assertThat(valve.getRemoteIpHeader()).isEqualTo("X-Forwarded-For");
				assertThat(valve.getProtocolHeader()).isEqualTo("X-Forwarded-Proto");
				assertThat(valve.getProtocolHeaderHttpsValue()).isEqualTo("https");
				assertThat(valve.getInternalProxies()).isEqualTo("192.168.0.1");
				assertThat(valve.getTrustedProxies()).isEqualTo("proxy1|proxy2");
			});
	}

	protected abstract F getFactory();

	protected abstract TomcatWebServer getWebServer(F factory);

}
