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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.ApplicationContextTestUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ShutdownEndpoint}.
 * 
 * @author Dave Syer
 */
public class ShutdownParentEndpointTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		ApplicationContextTestUtils.closeAll(this.context);
	}

	@Test
	public void shutdownChild() throws Exception {
		this.context = new SpringApplicationBuilder(Config.class).child(Empty.class)
				.web(false).run();
		CountDownLatch latch = this.context.getBean(Config.class).latch;
		assertThat((String) getEndpointBean().invoke().get("message"),
				startsWith("Shutting down"));
		assertTrue(this.context.isActive());
		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void shutdownParent() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class).child(Config.class)
				.web(false).run();
		CountDownLatch latch = this.context.getBean(Config.class).latch;
		assertThat((String) getEndpointBean().invoke().get("message"),
				startsWith("Shutting down"));
		assertTrue(this.context.isActive());
		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

	private ShutdownEndpoint getEndpointBean() {
		return this.context.getBean(ShutdownEndpoint.class);
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

	@Configuration
	public static class Empty {
	}
}
