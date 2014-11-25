/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class HealthMvcEndpointAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testSecureByDefault() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				TestHealthIndicator.class);
		this.context.refresh();
		Health health = (Health) this.context.getBean(HealthMvcEndpoint.class).invoke(
				null);
		assertEquals(Status.UP, health.getStatus());
		assertEquals(null, health.getDetails().get("foo"));
	}

	@Test
	public void testNotSecured() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				TestHealthIndicator.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled=false");
		this.context.refresh();
		Health health = (Health) this.context.getBean(HealthMvcEndpoint.class).invoke(
				null);
		assertEquals(Status.UP, health.getStatus());
		Health map = (Health) health.getDetails().get(
				"healthMvcEndpointAutoConfigurationTests.Test");
		assertEquals("bar", map.getDetails().get("foo"));
	}

	@Component
	protected static class TestHealthIndicator extends AbstractHealthIndicator {

		@Override
		protected void doHealthCheck(Builder builder) throws Exception {
			builder.up().withDetail("foo", "bar");
		}

	}

}
