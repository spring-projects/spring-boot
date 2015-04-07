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
import org.springframework.boot.actuate.endpoint.mvc.LogfileMvcEndpoint;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EndpointWebMvcAutoConfiguration} of the {@link LogfileMvcEndpoint}.
 *
 * @author Johannes Stelzer
 */
public class LogfileEndpointWebMvcAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void test_nologfile() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ManagementServerPropertiesAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.context.refresh();

		assertTrue(this.context.getBeansOfType(LogfileMvcEndpoint.class).isEmpty());
	}

	@Test
	public void test_logfile() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ManagementServerPropertiesAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class);

		EnvironmentTestUtils.addEnvironment(this.context, "logging.file:test.log");
		this.context.refresh();

		assertNotNull(this.context.getBean(LogfileMvcEndpoint.class));
	}

	@Test
	public void test_logpath() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(ManagementServerPropertiesAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class);

		EnvironmentTestUtils.addEnvironment(this.context, "logging.path:/var/log");
		this.context.refresh();

		assertNotNull(this.context.getBean(LogfileMvcEndpoint.class));
	}

}
