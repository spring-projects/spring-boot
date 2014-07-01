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

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.jmx.EndpointMBeanExporter;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
	public void testEndpointMBeanExporterIsInstalled() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, JmxAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(EndpointMBeanExporter.class));
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertFalse(mbeanExporter.getServer()
				.queryNames(getObjectName("*", "*,*", this.context), null).isEmpty());
	}

	@Test
	public void testEndpointMBeanExporterIsNotInstalledIfManagedResource()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, JmxAutoConfiguration.class,
				ManagedEndpoint.class, EndpointMBeanExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(EndpointMBeanExporter.class));

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertTrue(mbeanExporter.getServer()
				.queryNames(getObjectName("*", "*,*", this.context), null).isEmpty());
	}

	@Test
	public void testEndpointMBeanExporterIsNotInstalledIfNestedInManagedResource()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, JmxAutoConfiguration.class,
				NestedInManagedEndpoint.class,
				EndpointMBeanExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(EndpointMBeanExporter.class));

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertTrue(mbeanExporter.getServer()
				.queryNames(getObjectName("*", "*,*", this.context), null).isEmpty());
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void testEndpointMBeanExporterIsNotInstalled() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("endpoints.jmx.enabled", "false");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(environment);
		this.context.register(JmxAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(EndpointMBeanExporter.class);
		fail();
	}

	@Test
	public void testEndpointMBeanExporterWithProperties() throws IntrospectionException,
			InstanceNotFoundException, MalformedObjectNameException, ReflectionException {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("endpoints.jmx.domain", "test-domain");
		environment.setProperty("endpoints.jmx.unique_names", "true");
		environment.setProperty("endpoints.jmx.static_names", "key1=value1, key2=value2");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setEnvironment(environment);
		this.context.register(JmxAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(EndpointMBeanExporter.class);

		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				ObjectNameManager.getInstance(getObjectName("test-domain",
						"healthEndpoint", this.context).toString()
						+ ",key1=value1,key2=value2")));
	}

	@Test
	public void testEndpointMBeanExporterInParentChild() throws IntrospectionException,
			InstanceNotFoundException, MalformedObjectNameException, ReflectionException {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JmxAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class);

		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.register(JmxAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointMBeanExportAutoConfiguration.class);
		this.context.setParent(parent);

		parent.refresh();
		this.context.refresh();

		parent.close();

		System.out.println("parent " + ObjectUtils.getIdentityHexString(parent));
		System.out.println("child " + ObjectUtils.getIdentityHexString(this.context));
	}

	private ObjectName getObjectName(String domain, String beanKey,
			ApplicationContext applicationContext) throws MalformedObjectNameException {
		String name = "%s:type=Endpoint,name=%s";
		if (applicationContext.getParent() != null) {
			name = name + ",context=%s";
		}
		if (applicationContext.getEnvironment().getProperty("endpoints.jmx.unique_names",
				Boolean.class, false)) {
			name = name
					+ ",identity="
					+ ObjectUtils.getIdentityHexString(applicationContext
							.getBean(beanKey));
		}
		if (applicationContext.getParent() != null) {
			return ObjectNameManager.getInstance(String.format(name, domain, beanKey,
					ObjectUtils.getIdentityHexString(applicationContext)));
		}
		else {
			return ObjectNameManager.getInstance(String.format(name, domain, beanKey));
		}
	}

	@Configuration
	@EnableMBeanExport
	public static class TestConfiguration {

	}

	@Component
	@ManagedResource
	protected static class ManagedEndpoint extends AbstractEndpoint<Boolean> {

		public ManagedEndpoint() {
			super("managed", true, true);
		}

		@Override
		public Boolean invoke() {
			return true;
		}

	}

	@Configuration
	@ManagedResource
	protected static class NestedInManagedEndpoint {

		@Bean
		public Endpoint<Boolean> nested() {
			return new Nested();
		}

		class Nested extends AbstractEndpoint<Boolean> {

			public Nested() {
				super("managed", true, true);
			}

			@Override
			public Boolean invoke() {
				return true;
			}
		}

	}

}
