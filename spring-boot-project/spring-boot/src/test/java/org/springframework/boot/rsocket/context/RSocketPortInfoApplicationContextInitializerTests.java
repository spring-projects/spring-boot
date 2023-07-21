/*
 * Copyright 2012-2022 the original author or authors.
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
