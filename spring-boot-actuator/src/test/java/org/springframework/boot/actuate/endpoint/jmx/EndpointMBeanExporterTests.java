/*
 * Copyright 2012-2016 the original author or authors.
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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointMBeanExporter}
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
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
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		MBeanInfo mbeanInfo = mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint1", this.context));
		assertThat(mbeanInfo).isNotNull();
		assertThat(mbeanInfo.getOperations().length).isEqualTo(3);
		assertThat(mbeanInfo.getAttributes().length).isEqualTo(3);
	}

	@Test
	public void testSkipRegistrationOfDisabledEndpoint() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		MutablePropertyValues mpv = new MutablePropertyValues();
		mpv.add("enabled", Boolean.FALSE);
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class, null, mpv));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer()
				.isRegistered(getObjectName("endpoint1", this.context))).isFalse();
	}

	@Test
	public void testRegistrationOfEnabledEndpoint() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		MutablePropertyValues mpv = new MutablePropertyValues();
		mpv.add("enabled", Boolean.TRUE);
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class, null, mpv));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer()
				.isRegistered(getObjectName("endpoint1", this.context))).isTrue();
	}

	@Test
	public void testRegistrationTwoEndpoints() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.registerBeanDefinition("endpoint2",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint1", this.context))).isNotNull();
		assertThat(mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint2", this.context))).isNotNull();
	}

	@Test
	public void testRegistrationWithDifferentDomain() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(
								Collections.singletonMap("domain", "test-domain"))));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("test-domain", "endpoint1", false, this.context)))
						.isNotNull();
	}

	@Test
	public void testRegistrationWithDifferentDomainAndIdentity() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("domain", "test-domain");
		properties.put("ensureUniqueRuntimeObjectNames", true);
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(properties)));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer().getMBeanInfo(
				getObjectName("test-domain", "endpoint1", true, this.context)))
						.isNotNull();
	}

	@Test
	public void testRegistrationWithDifferentDomainAndIdentityAndStaticNames()
			throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("domain", "test-domain");
		properties.put("ensureUniqueRuntimeObjectNames", true);
		Properties staticNames = new Properties();
		staticNames.put("key1", "value1");
		staticNames.put("key2", "value2");
		properties.put("objectNameStaticProperties", staticNames);
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, null,
						new MutablePropertyValues(properties)));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer().getMBeanInfo(ObjectNameManager.getInstance(
				getObjectName("test-domain", "endpoint1", true, this.context).toString()
						+ ",key1=value1,key2=value2"))).isNotNull();
	}

	@Test
	public void testRegistrationWithParentContext() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(TestEndpoint.class));
		GenericApplicationContext parent = new GenericApplicationContext();
		this.context.setParent(parent);
		parent.refresh();
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		assertThat(mbeanExporter.getServer()
				.getMBeanInfo(getObjectName("endpoint1", this.context))).isNotNull();
		parent.close();
	}

	@Test
	public void jsonMapConversionWithDefaultObjectMapper() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(JsonMapConversionEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		Object response = mbeanExporter.getServer().invoke(
				getObjectName("endpoint1", this.context), "getData", new Object[0],
				new String[0]);
		assertThat(response).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) response).get("date")).isInstanceOf(Long.class);
	}

	@Test
	public void jsonMapConversionWithCustomObjectMapper() throws Exception {
		this.context = new GenericApplicationContext();
		ConstructorArgumentValues constructorArgs = new ConstructorArgumentValues();
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
		constructorArgs.addIndexedArgumentValue(0, objectMapper);
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class, constructorArgs,
						null));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(JsonMapConversionEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		Object response = mbeanExporter.getServer().invoke(
				getObjectName("endpoint1", this.context), "getData", new Object[0],
				new String[0]);
		assertThat(response).isInstanceOf(Map.class);
		assertThat(((Map<?, ?>) response).get("date")).isInstanceOf(String.class);
	}

	@Test
	public void jsonListConversion() throws Exception {
		this.context = new GenericApplicationContext();
		this.context.registerBeanDefinition("endpointMbeanExporter",
				new RootBeanDefinition(EndpointMBeanExporter.class));
		this.context.registerBeanDefinition("endpoint1",
				new RootBeanDefinition(JsonListConversionEndpoint.class));
		this.context.refresh();
		MBeanExporter mbeanExporter = this.context.getBean(EndpointMBeanExporter.class);
		Object response = mbeanExporter.getServer().invoke(
				getObjectName("endpoint1", this.context), "getData", new Object[0],
				new String[0]);
		assertThat(response).isInstanceOf(List.class);
		assertThat(((List<?>) response).get(0)).isInstanceOf(Long.class);
	}

	private ObjectName getObjectName(String beanKey, GenericApplicationContext context)
			throws MalformedObjectNameException {
		return getObjectName("org.springframework.boot", beanKey, false, context);
	}

	private ObjectName getObjectName(String domain, String beanKey,
			boolean includeIdentity, ApplicationContext applicationContext)
					throws MalformedObjectNameException {
		if (includeIdentity) {
			return ObjectNameManager.getInstance(String.format(
					"%s:type=Endpoint,name=%s,identity=%s", domain, beanKey, ObjectUtils
							.getIdentityHexString(applicationContext.getBean(beanKey))));
		}
		return ObjectNameManager
				.getInstance(String.format("%s:type=Endpoint,name=%s", domain, beanKey));
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

	public static class JsonMapConversionEndpoint
			extends AbstractEndpoint<Map<String, Object>> {

		public JsonMapConversionEndpoint() {
			super("json-map-conversion");
		}

		@Override
		public Map<String, Object> invoke() {
			Map<String, Object> result = new LinkedHashMap<String, Object>();
			result.put("date", new Date());
			return result;
		}

	}

	public static class JsonListConversionEndpoint
			extends AbstractEndpoint<List<Object>> {

		public JsonListConversionEndpoint() {
			super("json-list-conversion");
		}

		@Override
		public List<Object> invoke() {
			return Arrays.<Object>asList(new Date());
		}

	}

}
