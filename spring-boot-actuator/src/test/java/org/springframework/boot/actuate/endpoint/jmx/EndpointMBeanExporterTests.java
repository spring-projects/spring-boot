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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;

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
		this.context.registerBeanDefinition("mbeanExporter", new RootBeanDefinition(
				MBeanExporter.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(MBeanExporter.class);

		MBeanInfo mbeanInfo = mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint1", this.context));
		assertNotNull(mbeanInfo);
		assertEquals(3, mbeanInfo.getOperations().length);
		assertEquals(2, mbeanInfo.getAttributes().length);
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
		this.context.registerBeanDefinition("mbeanExporter", new RootBeanDefinition(
				MBeanExporter.class));
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
		this.context.registerBeanDefinition("mbeanExporter", new RootBeanDefinition(
				MBeanExporter.class));

		GenericApplicationContext parent = new GenericApplicationContext();
		parent.registerBeanDefinition("endpointMbeanExporter", new RootBeanDefinition(
				EndpointMBeanExporter.class));
		parent.registerBeanDefinition("endpoint1", new RootBeanDefinition(
				TestEndpoint.class));
		parent.registerBeanDefinition("mbeanExporter", new RootBeanDefinition(
				MBeanExporter.class));

		this.context.setParent(parent);
		parent.refresh();
		this.context.refresh();

		MBeanExporter mbeanExporter = parent.getBean(MBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint1", this.context)));
		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("endpoint1", parent)));

		parent.close();
	}

	@Test
	public void testRegistrationWithCustomDomainAndKey() throws Exception {
		Map<String, String> propertyValues = new HashMap<String, String>();
		propertyValues.put("domainName", "test.domain");

		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(propertyValues)));
		this.context.registerBeanDefinition("endpoint1", new RootBeanDefinition(
				TestEndpoint.class));
		this.context.registerBeanDefinition("mbeanExporter", new RootBeanDefinition(
				MBeanExporter.class));
		this.context.refresh();

		MBeanExporter mbeanExporter = this.context.getBean(MBeanExporter.class);

		assertNotNull(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("test.domain", "endpoint1", this.context,
						this.context.getBean("endpoint1"))));
	}

	private ObjectName getObjectName(String beanKey, ApplicationContext applicationContext)
			throws MalformedObjectNameException {
		return getObjectName("org.springframework.boot.actuate.endpoint", beanKey,
				applicationContext, applicationContext.getBean(beanKey));
	}

	private ObjectName getObjectName(String domainName, String beanKey,
			ApplicationContext applicationContext, Object object)
			throws MalformedObjectNameException {
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put("bean", beanKey);
		return JmxUtils.appendIdentityToObjectName(
				ObjectNameManager.getInstance(domainName, properties), object);
	}

	public static class TestEndpoint extends AbstractEndpoint<String> {

		public TestEndpoint() {
			super("/test");
		}

		@Override
		protected String doInvoke() {
			return "hello world";
		}
	}

}
