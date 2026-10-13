/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import java.util.function.Consumer;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatVirtualThreadsWebServerFactoryCustomizer}.
 *
 * @author Moritz Halbritter
 */
class TomcatVirtualThreadsWebServerFactoryCustomizerTests {

	private final TomcatVirtualThreadsWebServerFactoryCustomizer customizer = new TomcatVirtualThreadsWebServerFactoryCustomizer();

	@Test
	@EnabledForJreRange(min = JRE.JAVA_21)
	void shouldSetVirtualThreadExecutor() {
		withWebServer((webServer) -> assertThat(webServer.getTomcat().getConnector().getProtocolHandler().getExecutor())
			.isInstanceOf(VirtualThreadExecutor.class));
	}

	private TomcatWebServer getWebServer() {
		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		this.customizer.customize(factory);
		return (TomcatWebServer) factory.getWebServer();
	}

	private void withWebServer(Consumer<TomcatWebServer> callback) {
		TomcatWebServer webServer = getWebServer();
		webServer.start();
		try {
			callback.accept(webServer);
		}
		finally {
			webServer.stop();
		}
	}

}
