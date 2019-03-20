/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} serialization.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
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
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).hasSize(2);
		assertThat(map.get("name")).isEqualTo("foo");
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
		assertThat(nestedProperties).isNotNull();
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).hasSize(2);
		assertThat(((Map<String, Object>) map.get("bar")).get("name")).isEqualTo("foo");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSelfReferentialProperty() throws Exception {
		this.context.register(SelfReferentialConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.name:foo");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).containsOnlyKeys("bar", "name");
		assertThat(map).containsEntry("name", "foo");
		Map<String, Object> bar = (Map<String, Object>) map.get("bar");
		assertThat(bar).containsOnlyKeys("name");
		assertThat(bar).containsEntry("name", "123456");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCycle() {
		this.context.register(CycleConfig.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("cycle");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("cycle");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).containsOnlyKeys("error");
		assertThat(map).containsEntry("error", "Cannot serialize 'cycle'");
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
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).hasSize(3);
		assertThat(((Map<String, Object>) map.get("map")).get("name")).isEqualTo("foo");
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
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).hasSize(3);
		assertThat((map.get("map"))).isNull();
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
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).hasSize(3);
		assertThat(((List<String>) map.get("list")).get(0)).isEqualTo("foo");
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
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> map = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(map).isNotNull();
		assertThat(map).hasSize(3);
		assertThat(map.get("address")).isEqualTo("192.168.1.10");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInitializedMapAndList() throws Exception {
		this.context.register(InitializedMapAndListPropertiesConfig.class);
		EnvironmentTestUtils.addEnvironment(this.context, "foo.map.entryOne:true",
				"foo.list[0]:abc");
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = report.invoke();
		assertThat(properties).containsKeys("foo");
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("foo");
		assertThat(nestedProperties).containsOnlyKeys("prefix", "properties");
		assertThat(nestedProperties.get("prefix")).isEqualTo("foo");
		Map<String, Object> propertiesMap = (Map<String, Object>) nestedProperties
				.get("properties");
		assertThat(propertiesMap).containsOnlyKeys("bar", "name", "map", "list");
		Map<String, Object> map = (Map<String, Object>) propertiesMap.get("map");
		assertThat(map).containsOnly(entry("entryOne", true));
		List<String> list = (List<String>) propertiesMap.get("list");
		assertThat(list).containsExactly("abc");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void hikariDataSourceConfigurationPropertiesBeanCanBeSerialized() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(HikariDataSourceConfig.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint endpoint = this.context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
		Map<String, Object> properties = endpoint.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("hikariDataSource")).get("properties");
		assertThat(nestedProperties).doesNotContainKey("error");
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Base {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
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
	public static class SelfReferentialConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public SelfReferential foo() {
			return new SelfReferential();
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

	@Configuration
	@Import(Base.class)
	public static class InitializedMapAndListPropertiesConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		public InitializedMapAndListProperties foo() {
			return new InitializedMapAndListProperties();
		}

	}

	public static class Foo {

		private String name = "654321";

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

		public static class Bar {

			private String name = "123456";

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}

		}

	}

	public static class SelfReferential extends Foo {

		private Foo self;

		public SelfReferential() {
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

	public static class InitializedMapAndListProperties extends Foo {

		private Map<String, Boolean> map = new HashMap<String, Boolean>();

		private List<String> list = new ArrayList<String>();

		public Map<String, Boolean> getMap() {
			return this.map;
		}

		public List<String> getList() {
			return this.list;
		}

	}

	static class Cycle {

		private final Alpha alpha = new Alpha(this);

		public Alpha getAlpha() {
			return this.alpha;
		}

		static class Alpha {

			private final Cycle cycle;

			Alpha(Cycle cycle) {
				this.cycle = cycle;
			}

			public Cycle getCycle() {
				return this.cycle;
			}

		}

	}

	@Configuration
	@Import(Base.class)
	static class CycleConfig {

		@Bean
		@ConfigurationProperties(prefix = "cycle")
		public Cycle cycle() {
			return new Cycle();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	static class HikariDataSourceConfig {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		@ConfigurationProperties(prefix = "test.datasource")
		public HikariDataSource hikariDataSource() {
			return new HikariDataSource();
		}

	}

}
