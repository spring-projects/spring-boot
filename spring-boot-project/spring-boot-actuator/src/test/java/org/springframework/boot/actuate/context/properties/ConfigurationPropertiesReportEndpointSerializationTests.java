/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.context.properties;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
 * @author Andy Wilkinson
 */
public class ConfigurationPropertiesReportEndpointSerializationTests {

	@Test
	public void testNaming() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(FooConfig.class)
				.withPropertyValues("foo.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(2);
			assertThat(map.get("name")).isEqualTo("foo");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNestedNaming() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(FooConfig.class)
				.withPropertyValues("foo.bar.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo).isNotNull();
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(2);
			assertThat(((Map<String, Object>) map.get("bar")).get("name"))
					.isEqualTo("foo");
		});
	}

	@Test
	public void testCycle() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(CycleConfig.class)
				.withPropertyValues("foo.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(1);
			assertThat(map.get("error")).isEqualTo("Cannot serialize 'foo'");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testMap() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(MapConfig.class)
				.withPropertyValues("foo.map.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor fooProperties = properties.getBeans()
					.get("foo");
			assertThat(fooProperties).isNotNull();
			assertThat(fooProperties.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = fooProperties.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(3);
			assertThat(((Map<String, Object>) map.get("map")).get("name"))
					.isEqualTo("foo");
		});
	}

	@Test
	public void testEmptyMapIsNotAdded() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(MapConfig.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(2);
			assertThat(map).doesNotContainKey("map");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testList() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(ListConfig.class)
				.withPropertyValues("foo.list[0]:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(3);
			assertThat(((List<String>) map.get("list")).get(0)).isEqualTo("foo");
		});
	}

	@Test
	public void testInetAddress() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(AddressedConfig.class)
				.withPropertyValues("foo.address:192.168.1.10");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(3);
			assertThat(map.get("address")).isEqualTo("192.168.1.10");
		});
	}

	@Test
	@SuppressWarnings("unchecked")

	public void testInitializedMapAndList() throws Exception {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(InitializedMapAndListPropertiesConfig.class)
				.withPropertyValues("foo.map.entryOne:true", "foo.list[0]:abc");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor properties = endpoint
					.configurationProperties();
			assertThat(properties.getBeans()).containsKeys("foo");
			ConfigurationPropertiesBeanDescriptor foo = properties.getBeans().get("foo");
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> propertiesMap = foo.getProperties();
			assertThat(propertiesMap).containsOnlyKeys("bar", "name", "map", "list");
			Map<String, Object> map = (Map<String, Object>) propertiesMap.get("map");
			assertThat(map).containsOnly(entry("entryOne", true));
			List<String> list = (List<String>) propertiesMap.get("list");
			assertThat(list).containsExactly("abc");
		});
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

	public static class InitializedMapAndListProperties extends Foo {

		private Map<String, Boolean> map = new HashMap<>();

		private List<String> list = new ArrayList<>();

		public Map<String, Boolean> getMap() {
			return this.map;
		}

		public List<String> getList() {
			return this.list;
		}

	}

}
