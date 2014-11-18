/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetaData;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigurationPropertiesReportEndpointSerializationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNaming() throws Exception {
		this.context.register(FooConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		assertEquals("foo", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals("foo", map.get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedNaming() throws Exception {
		this.context.register(FooConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.bar.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals("foo", ((Map<String, Object>) map.get("bar")).get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCycle() throws Exception {
		this.context.register(CycleConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		assertEquals("foo", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(1, map.size());
		assertEquals("Cannot serialize 'foo'", map.get("error"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCycleAvoidedThroughManualMetadata() throws Exception {
		this.context.register(MetadataCycleConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "bar.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		assertEquals("bar", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(1, map.size());
		assertEquals("foo", map.get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMap() throws Exception {
		this.context.register(MapConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.map.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		assertEquals("foo", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(3, map.size());
		assertEquals("foo", ((Map<String, Object>) map.get("map")).get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEmptyMapIsNotAdded() throws Exception {
		this.context.register(MapConfig.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		System.err.println(nestedProperties);
		assertEquals("foo", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals(null, (map.get("map")));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testList() throws Exception {
		this.context.register(ListConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.list[0]:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		assertEquals("foo", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(3, map.size());
		assertEquals("foo", ((List<String>) map.get("list")).get(0));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMapWithMetadata() throws Exception {
		this.context.register(MetadataMapConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "spam.map.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		assertEquals("spam", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals("foo", ((Map<String, Object>) map.get("map")).get("name"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInetAddress() throws Exception {
		this.context.register(AddressedConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.address:192.168.1.10");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertNotNull(nestedProperties);
		System.err.println(nestedProperties);
		assertEquals("foo", nestedProperties.get("prefix"));
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertNotNull(map);
		assertEquals(3, map.size());
		assertEquals("192.168.1.10", map.get("address"));
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Base {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint(
				ConfigurationBeanFactoryMetaData beanFactoryMetaData) {
			ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint();
			endpoint.setConfigurationBeanFactoryMetaData(beanFactoryMetaData);
			return endpoint;
		}

	}

	@Configuration
	@Import(Base.class)
	public static class FooConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public Foo foo() {
			return new Foo();
		}

	}

	@Configuration
	@Import(Base.class)
	public static class CycleConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public Cycle foo() {
			return new Cycle();
		}

	}

	@Configuration
	@Import(Base.class)
	public static class MetadataCycleConfig {

		@Bean
		@ConfigurationProperties(prefix = "bar")
		public Cycle foo() {
			return new Cycle();
		}

	}

	@Configuration
	@Import(Base.class)
	public static class MapConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public MapHolder foo() {
			return new MapHolder();
		}

	}

	@Configuration
	@Import(Base.class)
	public static class ListConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public ListHolder foo() {
			return new ListHolder();
		}

	}

	@Configuration
	@Import(Base.class)
	public static class MetadataMapConfig {

		@Bean
		@ConfigurationProperties(prefix = "spam")
		public MapHolder foo() {
			return new MapHolder();
		}

	}

	@Configuration
	@Import(Base.class)
	public static class AddressedConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public Addressed foo() {
			return new Addressed();
		}

	}

	public static class Foo {

		private String name = "654321";

		public static class Bar {
			private String name = "123456";

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}

		private Bar bar = new Bar();

		public Bar getBar() {
			return this.bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		// No setter so it doesn't appear in the report
		public String getSummary() {
			return "Name: " + this.name;
		}

	}

	public static class Cycle extends Foo {

		private Foo self;

		public Cycle() {
			this.self = this;
		}

		public Foo getSelf() {
			return this.self;
		}

		public void setSelf(Foo self) {
			this.self = self;
		}
	}

	public static class MapHolder extends Foo {

		private Map<String, Object> map;

		public Map<String, Object> getMap() {
			return this.map;
		}

		public void setMap(Map<String, Object> map) {
			this.map = map;
		}
	}

	public static class ListHolder extends Foo {

		private List<String> list;

		public List<String> getList() {
			return this.list;
		}

		public void setList(List<String> list) {
			this.list = list;
		}
	}

	public static class Addressed extends Foo {

		private InetAddress address;

		public InetAddress getAddress() {
			return this.address;
		}

		public void setAddress(InetAddress address) {
			this.address = address;
		}

	}

}
