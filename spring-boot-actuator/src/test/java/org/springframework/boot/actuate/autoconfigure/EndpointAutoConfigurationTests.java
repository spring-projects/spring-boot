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

package org.springframework.boot.ops.autoconfigure;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.TestUtils;
import org.springframework.boot.ops.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.ops.endpoint.BeansEndpoint;
import org.springframework.boot.ops.endpoint.DumpEndpoint;
import org.springframework.boot.ops.endpoint.EnvironmentEndpoint;
import org.springframework.boot.ops.endpoint.HealthEndpoint;
import org.springframework.boot.ops.endpoint.InfoEndpoint;
import org.springframework.boot.ops.endpoint.MetricsEndpoint;
import org.springframework.boot.ops.endpoint.ShutdownEndpoint;
import org.springframework.boot.ops.endpoint.TraceEndpoint;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link EndpointAutoConfiguration}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class EndpointAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void endpoints() throws Exception {
		assertNotNull(this.context.getBean(BeansEndpoint.class));
		assertNotNull(this.context.getBean(DumpEndpoint.class));
		assertNotNull(this.context.getBean(EnvironmentEndpoint.class));
		assertNotNull(this.context.getBean(HealthEndpoint.class));
		assertNotNull(this.context.getBean(InfoEndpoint.class));
		assertNotNull(this.context.getBean(MetricsEndpoint.class));
		assertNotNull(this.context.getBean(ShutdownEndpoint.class));
		assertNotNull(this.context.getBean(TraceEndpoint.class));
	}

	@Test
	public void testInfoEndpointConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(this.context, "info.foo:bar");
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertNotNull(endpoint);
		assertNotNull(endpoint.invoke().get("git"));
		assertEquals("bar", endpoint.invoke().get("foo"));
	}

	@Test
	public void testNoGitProperties() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		TestUtils.addEnviroment(this.context,
				"spring.git.properties:classpath:nonexistent");
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertNotNull(endpoint);
		assertNull(endpoint.invoke().get("git"));
	}
}
