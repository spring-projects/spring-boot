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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanExporter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests for {@link EndpointMBeanExportAutoConfiguration}.
 * 
 */
public class EndpointMBeanExportAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testEndpointMBeanExporterIsInstalled() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(EndpointMBeanExporter.class));
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void testEndpointMBeanExporterIsNotInstalled() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(EndpointMBeanExporter.class);
		fail();
	}

	@Configuration
	@EnableMBeanExport
	public static class TestConfiguration {

	}
}
