/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.context;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.context.ShutdownEndpoint.ShutdownDescriptor;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ShutdownEndpoint}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class ShutdownEndpointTests {

	@Test
	void shutdown() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfig.class);
		contextRunner.run((context) -> {
			EndpointConfig config = context.getBean(EndpointConfig.class);
			ClassLoader previousTccl = Thread.currentThread().getContextClassLoader();
			ShutdownDescriptor result;
			Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[0], getClass().getClassLoader()));
			try {
				result = context.getBean(ShutdownEndpoint.class).shutdown();
			}
			finally {
				Thread.currentThread().setContextClassLoader(previousTccl);
			}
			assertThat(result.getMessage()).startsWith("Shutting down");
			assertThat(context.isActive()).isTrue();
			assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(config.threadContextClassLoader).isEqualTo(getClass().getClassLoader());
		});
	}

	@Test
	void shutdownChild() throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
			.child(EndpointConfig.class)
			.web(WebApplicationType.NONE)
			.run();
		CountDownLatch latch = context.getBean(EndpointConfig.class).latch;
		assertThat(context.getBean(ShutdownEndpoint.class).shutdown().getMessage()).startsWith("Shutting down");
		assertThat(context.isActive()).isTrue();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void shutdownParent() throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(EndpointConfig.class)
			.child(EmptyConfig.class)
			.web(WebApplicationType.NONE)
			.run();
		CountDownLatch parentLatch = context.getBean(EndpointConfig.class).latch;
		CountDownLatch childLatch = context.getBean(EmptyConfig.class).latch;
		assertThat(context.getBean(ShutdownEndpoint.class).shutdown().getMessage()).startsWith("Shutting down");
		assertThat(context.isActive()).isTrue();
		assertThat(parentLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(childLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfig {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile ClassLoader threadContextClassLoader;

		@Bean
		ShutdownEndpoint endpoint() {
			return new ShutdownEndpoint();
		}

		@Bean
		ApplicationListener<ContextClosedEvent> listener() {
			return (event) -> {
				EndpointConfig.this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
				EndpointConfig.this.latch.countDown();
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfig {

		private final CountDownLatch latch = new CountDownLatch(1);

		@Bean
		ApplicationListener<ContextClosedEvent> listener() {
			return (event) -> EmptyConfig.this.latch.countDown();
		}

	}

}
