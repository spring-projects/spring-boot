/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
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
public class ShutdownEndpointTests extends AbstractEndpointTests<ShutdownEndpoint> {

	public ShutdownEndpointTests() {
		super(Config.class, ShutdownEndpoint.class, "shutdown", true,
				"endpoints.shutdown");
	}

	@Override
	public void isEnabledByDefault() throws Exception {
		// Shutdown is dangerous so is disabled by default
		assertThat(getEndpointBean().isEnabled()).isFalse();
	}

	@Test
	public void invoke() throws Exception {
		Config config = this.context.getBean(Config.class);
		ClassLoader previousTccl = Thread.currentThread().getContextClassLoader();
		Map<String, Object> result;
		Thread.currentThread().setContextClassLoader(
				new URLClassLoader(new URL[0], getClass().getClassLoader()));
		try {
			result = getEndpointBean().invoke();
		}
		finally {
			Thread.currentThread().setContextClassLoader(previousTccl);
		}
		assertThat((String) result.get("message")).startsWith("Shutting down");
		assertThat(this.context.isActive()).isTrue();
		assertThat(config.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(config.threadContextClassLoader)
				.isEqualTo(getClass().getClassLoader());
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		private final CountDownLatch latch = new CountDownLatch(1);

		private volatile ClassLoader threadContextClassLoader;

		@Bean
		public ShutdownEndpoint endpoint() {
			ShutdownEndpoint endpoint = new ShutdownEndpoint();
			return endpoint;
		}

		@Bean
		public ApplicationListener<ContextClosedEvent> listener() {
			return new ApplicationListener<ContextClosedEvent>() {

				@Override
				public void onApplicationEvent(ContextClosedEvent event) {
					Config.this.threadContextClassLoader = Thread.currentThread()
							.getContextClassLoader();
					Config.this.latch.countDown();
				}

			};

		}

	}

}
