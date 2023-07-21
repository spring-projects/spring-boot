/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.context.properties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.InputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint} serialization.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesReportEndpointSerializationTests {

	@Test
	void testNaming() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(FooConfig.class)
			.withPropertyValues("foo.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(2);
			assertThat(map).containsEntry("name", "foo");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testNestedNaming() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(FooConfig.class)
			.withPropertyValues("foo.bar.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(foo).isNotNull();
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(2);
			assertThat(((Map<String, Object>) map.get("bar"))).containsEntry("name", "foo");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testSelfReferentialProperty() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(SelfReferentialConfig.class)
			.withPropertyValues("foo.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).containsOnlyKeys("bar", "name");
			assertThat(map).containsEntry("name", "foo");
			Map<String, Object> bar = (Map<String, Object>) map.get("bar");
			assertThat(bar).containsOnlyKeys("name");
			assertThat(bar).containsEntry("name", "123456");
		});
	}

	@Test
	void testCycle() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(CycleConfig.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor cycle = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("cycle");
			assertThat(cycle.getPrefix()).isEqualTo("cycle");
			Map<String, Object> map = cycle.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).containsOnlyKeys("error");
			assertThat(map).containsEntry("error", "Cannot serialize 'cycle'");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testMap() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(MapConfig.class)
			.withPropertyValues("foo.map.name:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor fooProperties = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(fooProperties).isNotNull();
			assertThat(fooProperties.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = fooProperties.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(3);
			assertThat(((Map<String, Object>) map.get("map"))).containsEntry("name", "foo");
		});
	}

	@Test
	void testEmptyMapIsNotAdded() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(MapConfig.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
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
	void testList() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withUserConfiguration(ListConfig.class)
			.withPropertyValues("foo.list[0]:foo");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(3);
			assertThat(((List<String>) map.get("list")).get(0)).isEqualTo("foo");
		});
	}

	@Test
	void testInetAddress() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(AddressedConfig.class)
			.withPropertyValues("foo.address:192.168.1.10");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(foo).isNotNull();
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> map = foo.getProperties();
			assertThat(map).isNotNull();
			assertThat(map).hasSize(3);
			assertThat(map).containsEntry("address", "192.168.1.10");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void testInitializedMapAndList() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(InitializedMapAndListPropertiesConfig.class)
			.withPropertyValues("foo.map.entryOne:true", "foo.list[0]:abc");
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor foo = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat(foo.getPrefix()).isEqualTo("foo");
			Map<String, Object> propertiesMap = foo.getProperties();
			assertThat(propertiesMap).containsOnlyKeys("bar", "name", "map", "list");
			Map<String, Object> map = (Map<String, Object>) propertiesMap.get("map");
			assertThat(map).containsOnly(entry("entryOne", true));
			List<String> list = (List<String>) propertiesMap.get("list");
			assertThat(list).containsExactly("abc");
		});
	}

	@Test
	void hikariDataSourceConfigurationPropertiesBeanCanBeSerialized() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(HikariDataSourceConfig.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor hikariDataSource = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("hikariDataSource");
			Map<String, Object> nestedProperties = hikariDataSource.getProperties();
			assertThat(nestedProperties).doesNotContainKey("error");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void endpointResponseUsesToStringOfCharSequenceAsPropertyValue() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withInitializer((context) -> {
			ConfigurableEnvironment environment = context.getEnvironment();
			environment.getPropertySources()
				.addFirst(new MapPropertySource("test",
						Collections.singletonMap("foo.name", new CharSequenceProperty("Spring Boot"))));
		}).withUserConfiguration(FooConfig.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor descriptor = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat((Map<String, Object>) descriptor.getInputs().get("name")).containsEntry("value", "Spring Boot");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void endpointResponseUsesPlaceholderForComplexValueAsPropertyValue() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner().withInitializer((context) -> {
			ConfigurableEnvironment environment = context.getEnvironment();
			environment.getPropertySources()
				.addFirst(new MapPropertySource("test",
						Collections.singletonMap("foo.name", new ComplexProperty("Spring Boot"))));
		}).withUserConfiguration(ComplexPropertyToStringConverter.class, FooConfig.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesDescriptor applicationProperties = endpoint.configurationProperties();
			ConfigurationPropertiesBeanDescriptor descriptor = applicationProperties.getContexts()
				.get(context.getId())
				.getBeans()
				.get("foo");
			assertThat((Map<String, Object>) descriptor.getInputs().get("name")).containsEntry("value",
					"Complex property value " + ComplexProperty.class.getName());
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class Base {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class FooConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		Foo foo() {
			return new Foo();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class SelfReferentialConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		SelfReferential foo() {
			return new SelfReferential();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class MetadataCycleConfig {

		@Bean
		@ConfigurationProperties(prefix = "bar")
		SelfReferential foo() {
			return new SelfReferential();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class MapConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		MapHolder foo() {
			return new MapHolder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class ListConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		ListHolder foo() {
			return new ListHolder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class MetadataMapConfig {

		@Bean
		@ConfigurationProperties(prefix = "spam")
		MapHolder foo() {
			return new MapHolder();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class AddressedConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		Addressed foo() {
			return new Addressed();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class InitializedMapAndListPropertiesConfig {

		@Bean
		@ConfigurationProperties(prefix = "foo")
		InitializedMapAndListProperties foo() {
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

		SelfReferential() {
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

		private final Map<String, Boolean> map = new HashMap<>();

		private final List<String> list = new ArrayList<>();

		public Map<String, Boolean> getMap() {
			return this.map;
		}

		public List<String> getList() {
			return this.list;
		}

	}

	public static class Cycle {

		private final Alpha alpha = new Alpha(this);

		public Alpha getAlpha() {
			return this.alpha;
		}

		public static class Alpha {

			private final Cycle cycle;

			Alpha(Cycle cycle) {
				this.cycle = cycle;
			}

			public Cycle getCycle() {
				return this.cycle;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(Base.class)
	static class CycleConfig {

		@Bean
		// gh-11037
		@ConfigurationProperties(prefix = "cycle")
		Cycle cycle() {
			return new Cycle();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class HikariDataSourceConfig {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint(Collections.emptyList(), Show.ALWAYS);
		}

		@Bean
		@ConfigurationProperties(prefix = "test.datasource")
		HikariDataSource hikariDataSource() {
			return new HikariDataSource();
		}

	}

	static class CharSequenceProperty implements CharSequence, InputStreamSource {

		private final String value;

		CharSequenceProperty(String value) {
			this.value = value;
		}

		@Override
		public int length() {
			return this.value.length();
		}

		@Override
		public char charAt(int index) {
			return this.value.charAt(index);
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return this.value.subSequence(start, end);
		}

		@Override
		public String toString() {
			return this.value;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(this.value.getBytes());
		}

	}

	static class ComplexProperty {

		private final String value;

		ComplexProperty(String value) {
			this.value = value;
		}

	}

	@ConfigurationPropertiesBinding
	static class ComplexPropertyToStringConverter implements Converter<ComplexProperty, String> {

		@Override
		public String convert(ComplexProperty source) {
			return source.value;
		}

	}

}
