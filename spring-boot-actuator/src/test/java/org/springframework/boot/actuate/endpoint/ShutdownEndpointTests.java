/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ShutdownEndpoint}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class ShutdownEndpointTests extends AbstractEndpointTests<ShutdownEndpoint> {

	public ShutdownEndpointTests() {
		super(Config.class, ShutdownEndpoint.class, "shutdown", true,
				"endpoints.shutdown");
	}

	@Test
	public void invoke() throws Exception {
		CountDownLatch latch = this.context.getBean(Config.class).latch;
		assertThat((String) getEndpointBean().invoke().get("message"),
				startsWith("Shutting down"));
		assertTrue(this.context.isActive());
		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		private CountDownLatch latch = new CountDownLatch(1);

		@Bean
		public ShutdownEndpoint endpoint() {
			ShutdownEndpoint endpoint = new ShutdownEndpoint();
			endpoint.setEnabled(true);
			return endpoint;
		}

		@Bean
		public ApplicationListener<ContextClosedEvent> listener() {
			return new ApplicationListener<ContextClosedEvent>() {
				@Override
				public void onApplicationEvent(ContextClosedEvent event) {
					Config.this.latch.countDown();
				}
			};

		}

	}
}
