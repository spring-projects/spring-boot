/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.jmx;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link JmxAutoConfiguration}
 * 
 * @author Christian Dupuis
 */
public class JmxAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultMBeanExport() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JmxAutoConfiguration.class);
		this.context.refresh();

		assertNotNull(this.context.getBean(MBeanExporter.class));
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void testDisabledMBeanExport() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.jmx.enabled", "false");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfiguration.class, JmxAutoConfiguration.class);
		this.context.refresh();

		this.context.getBean(MBeanExporter.class);
	}

	@Test
	public void testDefaultDomainConfiguredOnMBeanExport() {
		MockEnvironment env = new MockEnvironment();
		env.setProperty("spring.jmx.enabled", "true");
		env.setProperty("spring.jmx.default_domain", "my-test-domain");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(env);
		this.context.register(TestConfiguration.class, JmxAutoConfiguration.class);
		this.context.refresh();

		MBeanExporter mBeanExporter = this.context.getBean(MBeanExporter.class);
		assertNotNull(mBeanExporter);

		// TODO cdupuis add test for default domain
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public Counter counter() {
			return new Counter();
		}

		@ManagedResource
		public static class Counter {

			private int counter = 0;

			@ManagedAttribute
			public int get() {
				return this.counter;
			}

			@ManagedOperation
			public void increment() {
				this.counter++;
			}

		}

	}
}
