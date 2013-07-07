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

package org.springframework.zero.actuate.endpoint;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.actuate.endpoint.ShutdownEndpoint;
import org.springframework.zero.actuate.properties.ManagementServerProperties;
import org.springframework.zero.context.properties.EnableConfigurationProperties;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ShutdownEndpoint}.
 * 
 * @author Phillip Webb
 */
public class ShutdownEndpointTests extends AbstractEndpointTests<ShutdownEndpoint> {

	public ShutdownEndpointTests() {
		super(Config.class, ShutdownEndpoint.class, "/shutdown", true,
				"endpoints.shutdown");
	}

	@Test
	public void invoke() throws Exception {
		assertThat((String) getEndpointBean().invoke().get("message"),
				startsWith("Shutting down"));
		assertTrue(this.context.isActive());
		Thread.sleep(600);
		assertFalse(this.context.isActive());
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setAllowShutdown(true);
			return properties;
		}

		@Bean
		public ShutdownEndpoint endpoint() {
			return new ShutdownEndpoint();
		}

	}
}
