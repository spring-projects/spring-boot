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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
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
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void shutdownChild() throws Exception {
		this.context = new SpringApplicationBuilder(Config.class).child(Empty.class)
				.web(false).run();
		assertThat((String) getEndpointBean().invoke().get("message"),
				startsWith("Shutting down"));
		assertTrue(this.context.isActive());
		Thread.sleep(600);
		assertFalse(this.context.isActive());
	}

	@Test
	public void shutdownParent() throws Exception {
		this.context = new SpringApplicationBuilder(Empty.class).child(Config.class)
				.web(false).run();
		assertThat((String) getEndpointBean().invoke().get("message"),
				startsWith("Shutting down"));
		assertTrue(this.context.isActive());
		Thread.sleep(600);
		assertFalse(this.context.isActive());
	}

	private ShutdownEndpoint getEndpointBean() {
		return this.context.getBean(ShutdownEndpoint.class);
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public ShutdownEndpoint endpoint() {
			ShutdownEndpoint endpoint = new ShutdownEndpoint();
			endpoint.setEnabled(true);
			return endpoint;
		}

	}

	@Configuration
	public static class Empty {
	}
}
