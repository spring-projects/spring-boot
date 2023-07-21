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

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ContextConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author HaiTao Zhang
 * @author Chris Bono
 * @author Madhura Bhave
 */
@SuppressWarnings("unchecked")
class ConfigurationPropertiesReportEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(EndpointConfig.class);

	@Test
	void descriptorWithJavaBeanBindMethodDetectsRelevantProperties() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
			.run(assertProperties("test", (properties) -> assertThat(properties).containsOnlyKeys("dbPassword",
					"myTestProperty", "duration")));
	}

	@Test
	void descriptorWithAutowiredConstructorBindMethodDetectsRelevantProperties() {
		this.contextRunner.withUserConfiguration(AutowiredPropertiesConfiguration.class)
			.run(assertProperties("autowired", (properties) -> assertThat(properties).containsOnlyKeys("counter")));
	}

	@Test
	void descriptorWithValueObjectBindMethodDetectsRelevantProperties() {
		this.contextRunner.withUserConfiguration(ImmutablePropertiesConfiguration.class)
			.run(assertProperties("immutable",
					(properties) -> assertThat(properties).containsOnlyKeys("dbPassword", "myTestProperty", "for")));
	}

	@Test
	void descriptorWithValueObjectBindMethodUseDedicatedConstructor() {
		this.contextRunner.withUserConfiguration(MultiConstructorPropertiesConfiguration.class)
			.run(assertProperties("multiconstructor",
					(properties) -> assertThat(properties).containsOnly(entry("name", "test"))));
	}

	@Test
	void descriptorWithValueObjectBindMethodHandleNestedType() {
		this.contextRunner.withPropertyValues("immutablenested.nested.name=nested", "immutablenested.nested.counter=42")
			.withUserConfiguration(ImmutableNestedPropertiesConfiguration.class)
			.run(assertProperties("immutablenested", (properties) -> {
				assertThat(properties).containsOnlyKeys("name", "nested");
				Map<String, Object> nested = (Map<String, Object>) properties.get("nested");
				assertThat(nested).containsOnly(entry("name", "nested"), entry("counter", 42));
			}, (inputs) -> {
				Map<String, Object> nested = (Map<String, Object>) inputs.get("nested");
				Map<String, Object> name = (Map<String, Object>) nested.get("name");
				Map<String, Object> counter = (Map<String, Object>) nested.get("counter");
				assertThat(name).containsEntry("value", "nested");
				assertThat(name).containsEntry("origin",
						"\"immutablenested.nested.name\" from property source \"test\"");
				assertThat(counter).containsEntry("origin",
						"\"immutablenested.nested.counter\" from property source \"test\"");
				assertThat(counter).containsEntry("value", "42");
			}));
	}

	@Test
	void descriptorWithSimpleList() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
			.withPropertyValues("sensible.simpleList=a,b")
			.run(assertProperties("sensible", (properties) -> {
				assertThat(properties.get("simpleList")).isInstanceOf(List.class);
				List<String> list = (List<String>) properties.get("simpleList");
				assertThat(list).hasSize(2);
				assertThat(list.get(0)).isEqualTo("a");
				assertThat(list.get(1)).isEqualTo("b");
			}, (inputs) -> {
				List<Object> list = (List<Object>) inputs.get("simpleList");
				assertThat(list).hasSize(2);
				Map<String, String> item = (Map<String, String>) list.get(0);
				String origin = item.get("origin");
				String value = item.get("value");
				assertThat(value).isEqualTo("a,b");
				assertThat(origin).isEqualTo("\"sensible.simpleList\" from property source \"test\"");
			}));
	}

	@Test
	void descriptorDoesNotIncludePropertyWithNullValue() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
			.run(assertProperties("test", (properties) -> assertThat(properties).doesNotContainKey("nullValue")));
	}

	@Test
	void descriptorWithDurationProperty() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
			.run(assertProperties("test", (properties) -> assertThat(properties.get("duration"))
				.isEqualTo(Duration.ofSeconds(10).toString())));
	}

	@Test // gh-36076
	void descriptorWithWrapperProperty() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class).withInitializer((context) -> {
			ConfigurableEnvironment environment = context.getEnvironment();
			Map<String, Object> map = Collections.singletonMap("test.wrapper", 10);
			PropertySource<?> propertySource = new MapPropertySource("test", map);
			environment.getPropertySources().addLast(propertySource);
		})
			.run(assertProperties("test", (properties) -> assertThat(properties.get("wrapper")).isEqualTo(10),
					(inputs) -> {
						Map<String, Object> wrapper = (Map<String, Object>) inputs.get("wrapper");
						assertThat(wrapper.get("value")).isEqualTo(10);
					}));
	}

	@Test
	void descriptorWithNonCamelCaseProperty() {
		this.contextRunner.withUserConfiguration(MixedCasePropertiesConfiguration.class)
			.run(assertProperties("mixedcase",
					(properties) -> assertThat(properties.get("myURL")).isEqualTo("https://example.com")));
	}

	@Test
	void descriptorWithMixedCaseProperty() {
		this.contextRunner.withUserConfiguration(MixedCasePropertiesConfiguration.class)
			.run(assertProperties("mixedcase",
					(properties) -> assertThat(properties.get("mIxedCase")).isEqualTo("mixed")));
	}

	@Test
	void descriptorWithSingleLetterProperty() {
		this.contextRunner.withUserConfiguration(MixedCasePropertiesConfiguration.class)
			.run(assertProperties("mixedcase", (properties) -> assertThat(properties.get("z")).isEqualTo("zzz")));
	}

	@Test
	void descriptorWithSimpleBooleanProperty() {
		this.contextRunner.withUserConfiguration(BooleanPropertiesConfiguration.class)
			.run(assertProperties("boolean",
					(properties) -> assertThat(properties.get("simpleBoolean")).isEqualTo(true)));
	}

	@Test
	void descriptorWithMixedBooleanProperty() {
		this.contextRunner.withUserConfiguration(BooleanPropertiesConfiguration.class)
			.run(assertProperties("boolean",
					(properties) -> assertThat(properties.get("mixedBoolean")).isEqualTo(true)));
	}

	@Test
	void descriptorWithDataSizeProperty() {
		String configSize = "1MB";
		String stringifySize = DataSize.parse(configSize).toString();
		this.contextRunner.withUserConfiguration(DataSizePropertiesConfiguration.class)
			.withPropertyValues(String.format("data.size=%s", configSize))
			.run(assertProperties("data", (properties) -> assertThat(properties.get("size")).isEqualTo(stringifySize),
					(inputs) -> {
						Map<String, Object> size = (Map<String, Object>) inputs.get("size");
						assertThat(size).containsEntry("value", configSize);
						assertThat(size).containsEntry("origin", "\"data.size\" from property source \"test\"");
					}));
	}

	@Test
	void sanitizeLists() {
		new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfigWithShowNever.class, SensiblePropertiesConfiguration.class)
			.withPropertyValues("sensible.listItems[0].some-password=password")
			.run(assertProperties("sensible", (properties) -> {
				assertThat(properties.get("listItems")).isInstanceOf(List.class);
				List<Object> list = (List<Object>) properties.get("listItems");
				assertThat(list).hasSize(1);
				Map<String, Object> item = (Map<String, Object>) list.get(0);
				assertThat(item).containsEntry("somePassword", "******");
			}, (inputs) -> {
				List<Object> list = (List<Object>) inputs.get("listItems");
				assertThat(list).hasSize(1);
				Map<String, Object> item = (Map<String, Object>) list.get(0);
				Map<String, Object> somePassword = (Map<String, Object>) item.get("somePassword");
				assertThat(somePassword).containsEntry("value", "******");
				assertThat(somePassword).containsEntry("origin",
						"\"sensible.listItems[0].some-password\" from property source \"test\"");
			}));
	}

	@Test
	void listsOfListsAreSanitized() {
		new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfigWithShowNever.class, SensiblePropertiesConfiguration.class)
			.withPropertyValues("sensible.listOfListItems[0][0].some-password=password")
			.run(assertProperties("sensible", (properties) -> {
				assertThat(properties.get("listOfListItems")).isInstanceOf(List.class);
				List<List<Object>> listOfLists = (List<List<Object>>) properties.get("listOfListItems");
				assertThat(listOfLists).hasSize(1);
				List<Object> list = listOfLists.get(0);
				assertThat(list).hasSize(1);
				Map<String, Object> item = (Map<String, Object>) list.get(0);
				assertThat(item).containsEntry("somePassword", "******");
			}, (inputs) -> {
				assertThat(inputs.get("listOfListItems")).isInstanceOf(List.class);
				List<List<Object>> listOfLists = (List<List<Object>>) inputs.get("listOfListItems");
				assertThat(listOfLists).hasSize(1);
				List<Object> list = listOfLists.get(0);
				assertThat(list).hasSize(1);
				Map<String, Object> item = (Map<String, Object>) list.get(0);
				Map<String, Object> somePassword = (Map<String, Object>) item.get("somePassword");
				assertThat(somePassword).containsEntry("value", "******");
				assertThat(somePassword).containsEntry("origin",
						"\"sensible.listOfListItems[0][0].some-password\" from property source \"test\"");
			}));
	}

	@Test
	void sanitizeWithCustomSanitizingFunction() {
		new ApplicationContextRunner()
			.withUserConfiguration(CustomSanitizingEndpointConfig.class, SanitizingFunctionConfiguration.class,
					TestPropertiesConfiguration.class)
			.run(assertProperties("test", (properties) -> {
				assertThat(properties).containsEntry("dbPassword", "$$$");
				assertThat(properties).containsEntry("myTestProperty", "$$$");
			}));
	}

	@Test
	void sanitizeWithCustomPropertySourceBasedSanitizingFunction() {
		new ApplicationContextRunner()
			.withUserConfiguration(CustomSanitizingEndpointConfig.class,
					PropertySourceBasedSanitizingFunctionConfiguration.class, TestPropertiesConfiguration.class)
			.withPropertyValues("test.my-test-property=abcde")
			.run(assertProperties("test", (properties) -> {
				assertThat(properties).containsEntry("dbPassword", "123456");
				assertThat(properties).containsEntry("myTestProperty", "$$$");
			}));
	}

	@Test
	void sanitizeListsWithCustomSanitizingFunction() {
		new ApplicationContextRunner()
			.withUserConfiguration(CustomSanitizingEndpointConfig.class, SanitizingFunctionConfiguration.class,
					SensiblePropertiesConfiguration.class)
			.withPropertyValues("sensible.listItems[0].custom=my-value")
			.run(assertProperties("sensible", (properties) -> {
				assertThat(properties.get("listItems")).isInstanceOf(List.class);
				List<Object> list = (List<Object>) properties.get("listItems");
				assertThat(list).hasSize(1);
				Map<String, Object> item = (Map<String, Object>) list.get(0);
				assertThat(item).containsEntry("custom", "$$$");
			}, (inputs) -> {
				List<Object> list = (List<Object>) inputs.get("listItems");
				assertThat(list).hasSize(1);
				Map<String, Object> item = (Map<String, Object>) list.get(0);
				Map<String, Object> somePassword = (Map<String, Object>) item.get("custom");
				assertThat(somePassword).containsEntry("value", "$$$");
				assertThat(somePassword).containsEntry("origin",
						"\"sensible.listItems[0].custom\" from property source \"test\"");
			}));
	}

	@Test
	void noSanitizationWhenShowAlways() {
		new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfigWithShowAlways.class, TestPropertiesConfiguration.class)
			.run(assertProperties("test", (properties) -> {
				assertThat(properties).containsEntry("dbPassword", "123456");
				assertThat(properties).containsEntry("myTestProperty", "654321");
			}));
	}

	@Test
	void sanitizationWhenShowNever() {
		new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfigWithShowNever.class, TestPropertiesConfiguration.class)
			.run(assertProperties("test", (properties) -> {
				assertThat(properties).containsEntry("dbPassword", "******");
				assertThat(properties).containsEntry("myTestProperty", "******");
			}));
	}

	@Test
	void originParents() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
			.withInitializer(this::initializeOriginParents)
			.run(assertProperties("sensible", (properties) -> {
			}, (inputs) -> {
				Map<String, Object> stringInputs = (Map<String, Object>) inputs.get("string");
				String[] originParents = (String[]) stringInputs.get("originParents");
				assertThat(originParents).containsExactly("spring", "boot");
			}));
	}

	private void initializeOriginParents(ConfigurableApplicationContext context) {
		MockPropertySource propertySource = new OriginParentMockPropertySource();
		propertySource.setProperty("sensible.string", "spring");
		context.getEnvironment().getPropertySources().addFirst(propertySource);
	}

	private ContextConsumer<AssertableApplicationContext> assertProperties(String prefix,
			Consumer<Map<String, Object>> properties) {
		return assertProperties(prefix, properties, (inputs) -> {
		});
	}

	private ContextConsumer<AssertableApplicationContext> assertProperties(String prefix,
			Consumer<Map<String, Object>> properties, Consumer<Map<String, Object>> inputs) {
		return (context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
				.getBean(ConfigurationPropertiesReportEndpoint.class);
			ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor configurationProperties = endpoint
				.configurationProperties();
			ContextConfigurationPropertiesDescriptor allProperties = configurationProperties.getContexts()
				.get(context.getId());
			Optional<String> key = allProperties.getBeans()
				.keySet()
				.stream()
				.filter((id) -> findIdFromPrefix(prefix, id))
				.findAny();
			assertThat(key).describedAs("No configuration properties with prefix '%s' found", prefix).isPresent();
			ConfigurationPropertiesBeanDescriptor descriptor = allProperties.getBeans().get(key.get());
			assertThat(descriptor.getPrefix()).isEqualTo(prefix);
			properties.accept(descriptor.getProperties());
			inputs.accept(descriptor.getInputs());
		};
	}

	private boolean findIdFromPrefix(String prefix, String id) {
		int separator = id.indexOf("-");
		String candidate = (separator != -1) ? id.substring(0, separator) : id;
		return prefix.equals(candidate);
	}

	static class OriginParentMockPropertySource extends MockPropertySource implements OriginLookup<String> {

		@Override
		public Origin getOrigin(String key) {
			return new MockOrigin(key, new MockOrigin("spring", new MockOrigin("boot", null)));
		}

	}

	static class MockOrigin implements Origin {

		private final String value;

		private final MockOrigin parent;

		MockOrigin(String value, MockOrigin parent) {
			this.value = value;
			this.parent = parent;
		}

		@Override
		public Origin getParent() {
			return this.parent;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfig {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint(
					Collections.emptyList(), Show.WHEN_AUTHORIZED);
			return endpoint;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfigWithShowAlways {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint(
					Collections.emptyList(), Show.ALWAYS);
			return endpoint;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfigWithShowNever {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint() {
			ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint(
					Collections.emptyList(), Show.NEVER);
			return endpoint;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(TestProperties.class)
	static class TestPropertiesConfiguration {

	}

	@ConfigurationProperties(prefix = "test")
	public static class TestProperties {

		private String dbPassword = "123456";

		private String myTestProperty = "654321";

		private String nullValue = null;

		private Duration duration = Duration.ofSeconds(10);

		private final String ignored = "dummy";

		private Integer wrapper;

		public String getDbPassword() {
			return this.dbPassword;
		}

		public void setDbPassword(String dbPassword) {
			this.dbPassword = dbPassword;
		}

		public String getMyTestProperty() {
			return this.myTestProperty;
		}

		public void setMyTestProperty(String myTestProperty) {
			this.myTestProperty = myTestProperty;
		}

		public String getNullValue() {
			return this.nullValue;
		}

		public void setNullValue(String nullValue) {
			this.nullValue = nullValue;
		}

		public Duration getDuration() {
			return this.duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}

		public String getIgnored() {
			return this.ignored;
		}

		public Integer getWrapper() {
			return this.wrapper;
		}

		public void setWrapper(Integer wrapper) {
			this.wrapper = wrapper;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ImmutableProperties.class)
	static class ImmutablePropertiesConfiguration {

	}

	@ConfigurationProperties(prefix = "immutable")
	public static class ImmutableProperties {

		private final String dbPassword;

		private final String myTestProperty;

		private final String nullValue;

		private final Duration forDuration;

		private final String ignored;

		ImmutableProperties(@DefaultValue("123456") String dbPassword, @DefaultValue("654321") String myTestProperty,
				String nullValue, @DefaultValue("10s") @Name("for") Duration forDuration) {
			this.dbPassword = dbPassword;
			this.myTestProperty = myTestProperty;
			this.nullValue = nullValue;
			this.forDuration = forDuration;
			this.ignored = "dummy";
		}

		public String getDbPassword() {
			return this.dbPassword;
		}

		public String getMyTestProperty() {
			return this.myTestProperty;
		}

		public String getNullValue() {
			return this.nullValue;
		}

		public Duration getFor() {
			return this.forDuration;
		}

		public String getIgnored() {
			return this.ignored;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MultiConstructorProperties.class)
	static class MultiConstructorPropertiesConfiguration {

	}

	@ConfigurationProperties(prefix = "multiconstructor")
	public static class MultiConstructorProperties {

		private final String name;

		private final int counter;

		MultiConstructorProperties(String name, int counter) {
			this.name = name;
			this.counter = counter;
		}

		@ConstructorBinding
		MultiConstructorProperties(@DefaultValue("test") String name) {
			this.name = name;
			this.counter = 42;
		}

		public String getName() {
			return this.name;
		}

		public int getCounter() {
			return this.counter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AutowiredProperties.class)
	static class AutowiredPropertiesConfiguration {

		@Bean
		String hello() {
			return "hello";
		}

	}

	@ConfigurationProperties(prefix = "autowired")
	public static class AutowiredProperties {

		private final String name;

		private int counter;

		@Autowired
		AutowiredProperties(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public int getCounter() {
			return this.counter;
		}

		public void setCounter(int counter) {
			this.counter = counter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ImmutableNestedProperties.class)
	static class ImmutableNestedPropertiesConfiguration {

	}

	@ConfigurationProperties("immutablenested")
	public static class ImmutableNestedProperties {

		private final String name;

		private final Nested nested;

		ImmutableNestedProperties(@DefaultValue("parent") String name, Nested nested) {
			this.name = name;
			this.nested = nested;
		}

		public String getName() {
			return this.name;
		}

		public Nested getNested() {
			return this.nested;
		}

		public static class Nested {

			private final String name;

			private final int counter;

			Nested(String name, int counter) {
				this.name = name;
				this.counter = counter;
			}

			public String getName() {
				return this.name;
			}

			public int getCounter() {
				return this.counter;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(MixedCaseProperties.class)
	static class MixedCasePropertiesConfiguration {

	}

	@ConfigurationProperties("mixedcase")
	public static class MixedCaseProperties {

		private String myURL = "https://example.com";

		private String mIxedCase = "mixed";

		private String z = "zzz";

		public String getMyURL() {
			return this.myURL;
		}

		public void setMyURL(String myURL) {
			this.myURL = myURL;
		}

		public String getmIxedCase() {
			return this.mIxedCase;
		}

		public void setmIxedCase(String mIxedCase) {
			this.mIxedCase = mIxedCase;
		}

		public String getZ() {
			return this.z;
		}

		public void setZ(String z) {
			this.z = z;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(BooleanProperties.class)
	static class BooleanPropertiesConfiguration {

	}

	@ConfigurationProperties("boolean")
	public static class BooleanProperties {

		private boolean simpleBoolean = true;

		private Boolean mixedBoolean = true;

		public boolean isSimpleBoolean() {
			return this.simpleBoolean;
		}

		public void setSimpleBoolean(boolean simpleBoolean) {
			this.simpleBoolean = simpleBoolean;
		}

		public boolean isMixedBoolean() {
			return (this.mixedBoolean != null) ? this.mixedBoolean : false;
		}

		public void setMixedBoolean(Boolean mixedBoolean) {
			this.mixedBoolean = mixedBoolean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DataSizeProperties.class)
	static class DataSizePropertiesConfiguration {

	}

	@ConfigurationProperties("data")
	public static class DataSizeProperties {

		private DataSize size;

		public DataSize getSize() {
			return this.size;
		}

		public void setSize(DataSize size) {
			this.size = size;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(Gh4415Properties.class)
	static class Gh4415PropertiesConfiguration {

	}

	@ConfigurationProperties("gh4415")
	public static class Gh4415Properties {

		private Hidden hidden = new Hidden();

		private Map<String, Object> secrets = new HashMap<>();

		Gh4415Properties() {
			this.secrets.put("mine", "myPrivateThing");
			this.secrets.put("yours", "yourPrivateThing");
		}

		public Hidden getHidden() {
			return this.hidden;
		}

		public void setHidden(Hidden hidden) {
			this.hidden = hidden;
		}

		public Map<String, Object> getSecrets() {
			return this.secrets;
		}

		public void setSecrets(Map<String, Object> secrets) {
			this.secrets = secrets;
		}

		public static class Hidden {

			private String mine = "mySecret";

			public String getMine() {
				return this.mine;
			}

			public void setMine(String mine) {
				this.mine = mine;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(SensibleProperties.class)
	static class SensiblePropertiesConfiguration {

	}

	@ConfigurationProperties("sensible")
	public static class SensibleProperties {

		private String string;

		private URI sensitiveUri = URI.create("http://user:password@localhost:8080");

		private URI noPasswordUri = URI.create("http://user:@localhost:8080");

		private final List<String> simpleList = new ArrayList<>();

		private String rawSensitiveAddresses = "http://user:password@localhost:8080,http://user2:password2@localhost:8082";

		private List<ListItem> listItems = new ArrayList<>();

		private List<List<ListItem>> listOfListItems = new ArrayList<>();

		SensibleProperties() {
			this.listItems.add(new ListItem());
			this.listOfListItems.add(Collections.singletonList(new ListItem()));
		}

		public void setString(String string) {
			this.string = string;
		}

		public String getString() {
			return this.string;
		}

		public void setSensitiveUri(URI sensitiveUri) {
			this.sensitiveUri = sensitiveUri;
		}

		public URI getSensitiveUri() {
			return this.sensitiveUri;
		}

		public void setNoPasswordUri(URI noPasswordUri) {
			this.noPasswordUri = noPasswordUri;
		}

		public URI getNoPasswordUri() {
			return this.noPasswordUri;
		}

		public String getRawSensitiveAddresses() {
			return this.rawSensitiveAddresses;
		}

		public void setRawSensitiveAddresses(final String rawSensitiveAddresses) {
			this.rawSensitiveAddresses = rawSensitiveAddresses;
		}

		public List<ListItem> getListItems() {
			return this.listItems;
		}

		public void setListItems(List<ListItem> listItems) {
			this.listItems = listItems;
		}

		public List<List<ListItem>> getListOfListItems() {
			return this.listOfListItems;
		}

		public void setListOfListItems(List<List<ListItem>> listOfListItems) {
			this.listOfListItems = listOfListItems;
		}

		public List<String> getSimpleList() {
			return this.simpleList;
		}

		public static class ListItem {

			private String somePassword = "secret";

			private String custom;

			public String getSomePassword() {
				return this.somePassword;
			}

			public void setSomePassword(String somePassword) {
				this.somePassword = somePassword;
			}

			public String getCustom() {
				return this.custom;
			}

			public void setCustom(String custom) {
				this.custom = custom;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSanitizingEndpointConfig {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint(SanitizingFunction sanitizingFunction) {
			ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint(
					Collections.singletonList(sanitizingFunction), Show.ALWAYS);
			return endpoint;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SanitizingFunctionConfiguration {

		@Bean
		SanitizingFunction testSanitizingFunction() {
			return (data) -> {
				if (data.getKey().contains("custom") || data.getKey().contains("test")) {
					return data.withValue("$$$");
				}
				return data;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PropertySourceBasedSanitizingFunctionConfiguration {

		@Bean
		SanitizingFunction testSanitizingFunction() {
			return (data) -> {
				if (data.getPropertySource() != null && data.getPropertySource().getName().startsWith("test")) {
					return data.withValue("$$$");
				}
				return data;
			};
		}

	}

}
