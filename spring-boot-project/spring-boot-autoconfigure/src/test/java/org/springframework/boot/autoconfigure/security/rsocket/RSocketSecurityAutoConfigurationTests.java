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

package org.springframework.boot.autoconfigure.security.rsocket;

import io.rsocket.RSocketFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.rsocket.core.SecuritySocketAcceptorInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RSocketSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class RSocketSecurityAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(ReactiveUserDetailsServiceAutoConfiguration.class, RSocketSecurityAutoConfiguration.class,
					RSocketMessagingAutoConfiguration.class, RSocketStrategiesAutoConfiguration.class));

	@Test
	void autoConfigurationEnablesRSocketSecurity() {
		this.contextRunner.run((context) -> assertThat(context.getBean(RSocketSecurity.class)).isNotNull());
	}

	@Test
	void autoConfigurationIsConditionalOnSecuritySocketAcceptorInterceptorClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(SecuritySocketAcceptorInterceptor.class))
				.run((context) -> assertThat(context).doesNotHaveBean(RSocketSecurity.class));
	}

	@Test
	void autoConfigurationAddsCustomizerForServerRSocketFactory() {
		RSocketFactory.ServerRSocketFactory factory = mock(RSocketFactory.ServerRSocketFactory.class);
		ArgumentCaptor<SecuritySocketAcceptorInterceptor> captor = ArgumentCaptor
				.forClass(SecuritySocketAcceptorInterceptor.class);
		this.contextRunner.run((context) -> {
			ServerRSocketFactoryProcessor customizer = context.getBean(ServerRSocketFactoryProcessor.class);
			customizer.process(factory);
			verify(factory).addSocketAcceptorPlugin(captor.capture());
			assertThat(captor.getValue()).isInstanceOf(SecuritySocketAcceptorInterceptor.class);
		});
	}

}
