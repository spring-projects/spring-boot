/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.test.autoconfigure;

import io.grpc.Server;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.grpc.test.autoconfigure.GrpcServerPortInfoAutoConfiguration.GrpcPortInfoApplicationListener;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerPortInfoAutoConfiguration}.
 *
 * @author Phillip Webb
 */
class GrpcServerPortInfoAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcServerPortInfoAutoConfiguration.class));

	private static final String PORT_PROPERTY = "local.grpc.server.port";

	@Test
	void createsGrpcPortInfoApplicationListenerBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(GrpcPortInfoApplicationListener.class));
	}

	@Test
	void whenNoGrpcServerStartedEventClassDoesNotCreateBean() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerStartedEvent.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcPortInfoApplicationListener.class));
	}

	@Test
	void whenServerHasAddressListenerSetsPortProperty() {
		NettyGrpcServerFactory factory = mock();
		testListener(factory, 65535, "65535");
	}

	@Test
	void whenServerHasNoAddressListenerSetsNoPortProperty() {
		NettyGrpcServerFactory factory = mock();
		testListener(factory, -1, null);
	}

	@Test
	void whenInProcessGrpcServerFactorySetsNoPortProperty() {
		InProcessGrpcServerFactory factory = mock();
		testListener(factory, 65535, null);
	}

	@Test
	void whenTestGrpcServerFactorySetsNoPortProperty() {
		TestGrpcServerFactory factory = mock();
		testListener(factory, 65535, null);
	}

	private void testListener(GrpcServerFactory factory, int port, @Nullable String expected) {
		this.contextRunner.run((context) -> {
			GrpcServerLifecycle lifecycle = mock();
			Server server = mock();
			given(lifecycle.getFactory()).willReturn(factory);
			GrpcServerStartedEvent event = new GrpcServerStartedEvent(lifecycle, server, "localhost", port);
			context.publishEvent(event);
			assertThat(context.getEnvironment().getProperty(PORT_PROPERTY)).isEqualTo(expected);
		});
	}

}
