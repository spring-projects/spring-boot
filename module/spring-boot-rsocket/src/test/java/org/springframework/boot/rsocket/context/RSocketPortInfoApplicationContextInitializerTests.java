/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rsocket.context;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RSocketPortInfoApplicationContextInitializer}.
 *
 * @author Spencer Gibb
 * @author Andy Wilkinson
 */
class RSocketPortInfoApplicationContextInitializerTests {

	@Test
	void whenServerHasAddressThenInitializerSetsPortProperty() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
			context.getBean(RSocketPortInfoApplicationContextInitializer.class).initialize(context);
			RSocketServer server = mock(RSocketServer.class);
			given(server.address()).willReturn(new InetSocketAddress(65535));
			context.publishEvent(new RSocketServerInitializedEvent(server));
			assertThat(context.getEnvironment().getProperty("local.rsocket.server.port")).isEqualTo("65535");
		}
	}

	@Test
	void whenServerHasNoAddressThenInitializerDoesNotSetPortProperty() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
			context.getBean(RSocketPortInfoApplicationContextInitializer.class).initialize(context);
			RSocketServer server = mock(RSocketServer.class);
			context.publishEvent(new RSocketServerInitializedEvent(server));
			then(server).should().address();
			assertThat(context.getEnvironment().getProperty("local.rsocket.server.port")).isNull();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		RSocketPortInfoApplicationContextInitializer rSocketPortInfoApplicationContextInitializer() {
			return new RSocketPortInfoApplicationContextInitializer();
		}

	}

}
