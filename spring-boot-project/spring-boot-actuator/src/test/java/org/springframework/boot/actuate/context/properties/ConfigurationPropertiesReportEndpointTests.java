/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ContextConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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
 */
class ConfigurationPropertiesReportEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(EndpointConfig.class);

	@Test
	void descriptorWithJavaBeanBindMethodDetectsRelevantProperties() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class).run(assertProperties("test",
				(properties) -> assertThat(properties).containsOnlyKeys("dbPassword", "myTestProperty", "duration")));
	}

	@Test
	void descriptorWithValueObjectBindMethodDetectsRelevantProperties() {
		this.contextRunner.withUserConfiguration(ImmutablePropertiesConfiguration.class).run(assertProperties(
				"immutable",
				(properties) -> assertThat(properties).containsOnlyKeys("dbPassword", "myTestProperty", "duration")));
	}

	@Test
	void descriptorWithValueObjectBindMethodUseDedicatedConstructor() {
		this.contextRunner.withUserConfiguration(MultiConstructorPropertiesConfiguration.class).run(assertProperties(
				"multiconstructor", (properties) -> assertThat(properties).containsOnly(entry("name", "test"))));
	}

	@Test
	void descriptorWithValueObjectBindMethodHandleNestedType() {
		this.contextRunner.withPropertyValues("immutablenested.nested.name=nested", "immutablenested.nested.counter=42")
				.withUserConfiguration(ImmutableNestedPropertiesConfiguration.class)
				.run(assertProperties("immutablenested", (properties) -> {
					assertThat(properties).containsOnlyKeys("name", "nested");
					Map<String, Object> nested = (Map<String, Object>) properties.get("nested");
					assertThat(nested).containsOnly(entry("name", "nested"), entry("counter", 42));
				}));
	}

	@Test
	void descriptorDoesNotIncludePropertyWithNullValue() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
				.run(assertProperties("test", (properties) -> assertThat(properties).doesNotContainKey("nullValue")));
	}

	@Test
	void descriptorWithDurationProperty() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class).run(assertProperties("test",
				(properties) -> assertThat(properties.get("duration")).isEqualTo(Duration.ofSeconds(10).toString())));
	}

	@Test
	void descriptorWithNonCamelCaseProperty() {
		this.contextRunner.withUserConfiguration(MixedCasePropertiesConfiguration.class).run(assertProperties(
				"mixedcase", (properties) -> assertThat(properties.get("myURL")).isEqualTo("https://example.com")));
	}

	@Test
	void descriptorWithMixedCaseProperty() {
		this.contextRunner.withUserConfiguration(MixedCasePropertiesConfiguration.class).run(assertProperties(
				"mixedcase", (properties) -> assertThat(properties.get("mIxedCase")).isEqualTo("mixed")));
	}

	@Test
	void descriptorWithSingleLetterProperty() {
		this.contextRunner.withUserConfiguration(MixedCasePropertiesConfiguration.class)
				.run(assertProperties("mixedcase", (properties) -> assertThat(properties.get("z")).isEqualTo("zzz")));
	}

	@Test
	void descriptorWithSimpleBooleanProperty() {
		this.contextRunner.withUserConfiguration(BooleanPropertiesConfiguration.class).run(assertProperties("boolean",
				(properties) -> assertThat(properties.get("simpleBoolean")).isEqualTo(true)));
	}

	@Test
	void descriptorWithMixedBooleanProperty() {
		this.contextRunner.withUserConfiguration(BooleanPropertiesConfiguration.class).run(assertProperties("boolean",
				(properties) -> assertThat(properties.get("mixedBoolean")).isEqualTo(true)));
	}

	@Test
	void sanitizeWithDefaultSettings() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
				.run(assertProperties("test", (properties) -> {
					assertThat(properties.get("dbPassword")).isEqualTo("******");
					assertThat(properties.get("myTestProperty")).isEqualTo("654321");
				}));
	}

	@Test
	void sanitizeWithCustomKey() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
				.withPropertyValues("test.keys-to-sanitize=property").run(assertProperties("test", (properties) -> {
					assertThat(properties.get("dbPassword")).isEqualTo("123456");
					assertThat(properties.get("myTestProperty")).isEqualTo("******");
				}));
	}

	@Test
	void sanitizeWithCustomKeyPattern() {
		this.contextRunner.withUserConfiguration(TestPropertiesConfiguration.class)
				.withPropertyValues("test.keys-to-sanitize=.*pass.*").run(assertProperties("test", (properties) -> {
					assertThat(properties.get("dbPassword")).isEqualTo("******");
					assertThat(properties.get("myTestProperty")).isEqualTo("654321");
				}));
	}

	@Test
	void sanitizeWithCustomPatternUsingCompositeKeys() {
		this.contextRunner.withUserConfiguration(Gh4415PropertiesConfiguration.class)
				.withPropertyValues("test.keys-to-sanitize=.*\\.secrets\\..*,.*\\.hidden\\..*")
				.run(assertProperties("gh4415", (properties) -> {
					Map<String, Object> secrets = (Map<String, Object>) properties.get("secrets");
					Map<String, Object> hidden = (Map<String, Object>) properties.get("hidden");
					assertThat(secrets.get("mine")).isEqualTo("******");
					assertThat(secrets.get("yours")).isEqualTo("******");
					assertThat(hidden.get("mine")).isEqualTo("******");
				}));
	}

	@Test
	void sanitizeUriWithSensitiveInfo() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
				.run(assertProperties("sensible", (properties) -> assertThat(properties.get("sensitiveUri"))
						.isEqualTo("http://user:******@localhost:8080")));
	}

	@Test
	void sanitizeUriWithNoPassword() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
				.run(assertProperties("sensible", (properties) -> assertThat(properties.get("noPasswordUri"))
						.isEqualTo("http://user:******@localhost:8080")));
	}

	@Test
	void sanitizeAddressesFieldContainingMultipleRawSensitiveUris() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
				.run(assertProperties("sensible", (properties) -> assertThat(properties.get("rawSensitiveAddresses"))
						.isEqualTo("http://user:******@localhost:8080,http://user2:******@localhost:8082")));
	}

	@Test
	void sanitizeLists() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
				.run(assertProperties("sensible", (properties) -> {
					assertThat(properties.get("listItems")).isInstanceOf(List.class);
					List<Object> list = (List<Object>) properties.get("listItems");
					assertThat(list).hasSize(1);
					Map<String, Object> item = (Map<String, Object>) list.get(0);
					assertThat(item.get("somePassword")).isEqualTo("******");
				}));
	}

	@Test
	void listsOfListsAreSanitized() {
		this.contextRunner.withUserConfiguration(SensiblePropertiesConfiguration.class)
				.run(assertProperties("sensible", (properties) -> {
					assertThat(properties.get("listOfListItems")).isInstanceOf(List.class);
					List<List<Object>> listOfLists = (List<List<Object>>) properties.get("listOfListItems");
					assertThat(listOfLists).hasSize(1);
					List<Object> list = listOfLists.get(0);
					assertThat(list).hasSize(1);
					Map<String, Object> item = (Map<String, Object>) list.get(0);
					assertThat(item.get("somePassword")).isEqualTo("******");
				}));
	}

	private ContextConsumer<AssertableApplicationContext> assertProperties(String prefix,
			Consumer<Map<String, Object>> properties) {
		return (context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			ContextConfigurationProperties allProperties = endpoint.configurationProperties().getContexts()
					.get(context.getId());
			Optional<String> key = allProperties.getBeans().keySet().stream()
					.filter((id) -> findIdFromPrefix(prefix, id)).findAny();
			assertThat(key).describedAs("No configuration properties with prefix '%s' found", prefix).isPresent();
			ConfigurationPropertiesBeanDescriptor descriptor = allProperties.getBeans().get(key.get());
			assertThat(descriptor.getPrefix()).isEqualTo(prefix);
			properties.accept(descriptor.getProperties());
		};
	}

	private boolean findIdFromPrefix(String prefix, String id) {
		int separator = id.indexOf("-");
		String candidate = (separator != -1) ? id.substring(0, separator) : id;
		return prefix.equals(candidate);
	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfig {

		@Bean
		ConfigurationPropertiesReportEndpoint endpoint(Environment environment) {
			ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint();
			String[] keys = environment.getProperty("test.keys-to-sanitize", String[].class);
			if (keys != null) {
				endpoint.setKeysToSanitize(keys);
			}
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

		private String ignored = "dummy";

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

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ImmutableProperties.class)
	static class ImmutablePropertiesConfiguration {

	}

	@ConfigurationProperties(prefix = "immutable")
	@ConstructorBinding
	public static class ImmutableProperties {

		private final String dbPassword;

		private final String myTestProperty;

		private final String nullValue;

		private final Duration duration;

		private final String ignored;

		ImmutableProperties(@DefaultValue("123456") String dbPassword, @DefaultValue("654321") String myTestProperty,
				String nullValue, @DefaultValue("10s") Duration duration) {
			this.dbPassword = dbPassword;
			this.myTestProperty = myTestProperty;
			this.nullValue = nullValue;
			this.duration = duration;
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

		public Duration getDuration() {
			return this.duration;
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
	@ConstructorBinding
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
	@EnableConfigurationProperties(ImmutableNestedProperties.class)
	static class ImmutableNestedPropertiesConfiguration {

	}

	@ConfigurationProperties("immutablenested")
	@ConstructorBinding
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

		private URI sensitiveUri = URI.create("http://user:password@localhost:8080");

		private URI noPasswordUri = URI.create("http://user:@localhost:8080");

		private String rawSensitiveAddresses = "http://user:password@localhost:8080,http://user2:password2@localhost:8082";

		private List<ListItem> listItems = new ArrayList<>();

		private List<List<ListItem>> listOfListItems = new ArrayList<>();

		SensibleProperties() {
			this.listItems.add(new ListItem());
			this.listOfListItems.add(Collections.singletonList(new ListItem()));
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

		public static class ListItem {

			private String somePassword = "secret";

			public String getSomePassword() {
				return this.somePassword;
			}

			public void setSomePassword(String somePassword) {
				this.somePassword = somePassword;
			}

		}

	}

}
