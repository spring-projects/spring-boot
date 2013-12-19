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

package org.springframework.boot.actuate.endpoint.jmx;

import java.util.Collections;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.MBeanExporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link EndpointMBeanExporter}
 * 
 * @author Christian Dupuis
 */
public class EndpointMBeanExporterTests {

	GenericApplicationContext context = null;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testRegistrationOfOneEndpoint() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1", new RootBeanDefinition(
				TestEndpoint.class));
		this.context.registerBeanDefinition(
				"mbeanExporter",
				new RootBeanDefinition(MBeanExporter.class, null,
						new MutablePropertyValues(Collections.singletonMap(
								"ensureUniqueRuntimeObjectNames", "false"))));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(MBeanExporter.class);

		MBeanInfo mbeanInfo = mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint1", this.context));
		assertNotNull(mbeanInfo);
		assertEquals(5, mbeanInfo.getOperations().length);
		assertEquals(5, mbeanInfo.getAttributes().length);
	}

	@Test
	public void testRegistrationTwoEndpoints() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1", new RootBeanDefinition(
				TestEndpoint.class));
		this.context.registerBeanDefinition("endpoint2", new RootBeanDefinition(
				TestEndpoint.class));
		this.context.registerBeanDefinition(
				"mbeanExporter",
				new RootBeanDefinition(MBeanExporter.class, null,
						new MutablePropertyValues(Collections.singletonMap(
								"ensureUniqueRuntimeObjectNames", "false"))));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(MBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint1", this.context)));
		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint2", this.context)));
	}

	@Test
	public void testRegistrationWithParentContext() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1", new RootBeanDefinition(
				TestEndpoint.class));
		this.context.registerBeanDefinition(
				"mbeanExporter",
				new RootBeanDefinition(MBeanExporter.class, null,
						new MutablePropertyValues(Collections.singletonMap(
								"ensureUniqueRuntimeObjectNames", "false"))));

		GenericApplicationContext parent = new GenericApplicationContext();

		this.context.setParent(parent);
		parent.refresh();
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(MBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint1", this.context)));

		parent.close();
	}

	private ObjectName getObjectName(String beanKey, ApplicationContext applicationContext)
			throws MalformedObjectNameException {
		return new DataEndpointMBean(beanKey,
				(Endpoint<?>) applicationContext.getBean(beanKey)).getObjectName();
	}

	public static class TestEndpoint extends AbstractEndpoint<String> {

		public TestEndpoint() {
			super("test");
		}

		@Override
		public String invoke() {
			return "hello world";
		}
	}

}
