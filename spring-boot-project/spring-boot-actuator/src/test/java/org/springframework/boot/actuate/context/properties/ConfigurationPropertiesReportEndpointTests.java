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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class ConfigurationPropertiesReportEndpointTests {

	@Test
	public void configurationPropertiesAreReturned() throws Exception {
		load((context, properties) -> {
			assertThat(properties.getContextId()).isEqualTo(context.getId());
			assertThat(properties.getBeans().size()).isGreaterThan(0);
			ConfigurationPropertiesBeanDescriptor nestedProperties = properties.getBeans()
					.get("testProperties");
			assertThat(nestedProperties).isNotNull();
			assertThat(nestedProperties.getPrefix()).isEqualTo("test");
			assertThat(nestedProperties.getProperties()).isNotEmpty();
		});
	}

	@Test
	public void entriesWithNullValuesAreNotIncluded() {
		load((context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties).doesNotContainKey("nullValue");
		});
	}

	@Test
	public void defaultKeySanitization() throws Exception {
		load((context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties).isNotNull();
			assertThat(nestedProperties.get("dbPassword")).isEqualTo("******");
			assertThat(nestedProperties.get("myTestProperty")).isEqualTo("654321");
		});
	}

	@Test
	public void customKeySanitization() throws Exception {
		load("property", (context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties).isNotNull();
			assertThat(nestedProperties.get("dbPassword")).isEqualTo("123456");
			assertThat(nestedProperties.get("myTestProperty")).isEqualTo("******");
		});
	}

	@Test
	public void customPatternKeySanitization() throws Exception {
		load(".*pass.*", (context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties).isNotNull();
			assertThat(nestedProperties.get("dbPassword")).isEqualTo("******");
			assertThat(nestedProperties.get("myTestProperty")).isEqualTo("654321");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void keySanitizationWithCustomPatternUsingCompositeKeys() throws Exception {
		// gh-4415
		load(Arrays.asList(".*\\.secrets\\..*", ".*\\.hidden\\..*"),
				(context, properties) -> {
					Map<String, Object> nestedProperties = properties.getBeans()
							.get("testProperties").getProperties();
					assertThat(nestedProperties).isNotNull();
					Map<String, Object> secrets = (Map<String, Object>) nestedProperties
							.get("secrets");
					Map<String, Object> hidden = (Map<String, Object>) nestedProperties
							.get("hidden");
					assertThat(secrets.get("mine")).isEqualTo("******");
					assertThat(secrets.get("yours")).isEqualTo("******");
					assertThat(hidden.get("mine")).isEqualTo("******");
				});
	}

	@Test
	public void mixedBoolean() throws Exception {
		load((context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties.get("mixedBoolean")).isEqualTo(true);
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listsAreSanitized() throws Exception {
		load((context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties.get("listItems")).isInstanceOf(List.class);
			List<Object> list = (List<Object>) nestedProperties.get("listItems");
			assertThat(list).hasSize(1);
			Map<String, Object> item = (Map<String, Object>) list.get(0);
			assertThat(item.get("somePassword")).isEqualTo("******");
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listsOfListsAreSanitized() throws Exception {
		load((context, properties) -> {
			Map<String, Object> nestedProperties = properties.getBeans()
					.get("testProperties").getProperties();
			assertThat(nestedProperties.get("listOfListItems")).isInstanceOf(List.class);
			List<List<Object>> listOfLists = (List<List<Object>>) nestedProperties
					.get("listOfListItems");
			assertThat(listOfLists).hasSize(1);
			List<Object> list = listOfLists.get(0);
			assertThat(list).hasSize(1);
			Map<String, Object> item = (Map<String, Object>) list.get(0);
			assertThat(item.get("somePassword")).isEqualTo("******");
		});
	}

	private void load(
			BiConsumer<ApplicationContext, ConfigurationPropertiesDescriptor> properties) {
		load(Collections.emptyList(), properties);
	}

	private void load(String keyToSanitize,
			BiConsumer<ApplicationContext, ConfigurationPropertiesDescriptor> properties) {
		load(Collections.singletonList(keyToSanitize), properties);
	}

	private void load(List<String> keysToSanitize,
			BiConsumer<ApplicationContext, ConfigurationPropertiesDescriptor> properties) {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(Config.class);
		contextRunner.run((context) -> {
			ConfigurationPropertiesReportEndpoint endpoint = context
					.getBean(ConfigurationPropertiesReportEndpoint.class);
			if (!CollectionUtils.isEmpty(keysToSanitize)) {
				endpoint.setKeysToSanitize(
						keysToSanitize.toArray(new String[keysToSanitize.size()]));
			}
			properties.accept(context, endpoint.configurationProperties());
		});
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Parent {

		@Bean
		public TestProperties testProperties() {
			return new TestProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		@Bean
		public ConfigurationPropertiesReportEndpoint endpoint() {
			return new ConfigurationPropertiesReportEndpoint();
		}

		@Bean
		public TestProperties testProperties() {
			return new TestProperties();
		}

	}

	@ConfigurationProperties(prefix = "test")
	public static class TestProperties {

		private String dbPassword = "123456";

		private String myTestProperty = "654321";

		private Boolean mixedBoolean = true;

		private Map<String, Object> secrets = new HashMap<>();

		private Hidden hidden = new Hidden();

		private List<ListItem> listItems = new ArrayList<>();

		private List<List<ListItem>> listOfListItems = new ArrayList<>();

		private String nullValue = null;

		public TestProperties() {
			this.secrets.put("mine", "myPrivateThing");
			this.secrets.put("yours", "yourPrivateThing");
			this.listItems.add(new ListItem());
			this.listOfListItems.add(Arrays.asList(new ListItem()));
		}

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

		public boolean isMixedBoolean() {
			return (this.mixedBoolean == null ? false : this.mixedBoolean);
		}

		public void setMixedBoolean(Boolean mixedBoolean) {
			this.mixedBoolean = mixedBoolean;
		}

		public Map<String, Object> getSecrets() {
			return this.secrets;
		}

		public void setSecrets(Map<String, Object> secrets) {
			this.secrets = secrets;
		}

		public Hidden getHidden() {
			return this.hidden;
		}

		public void setHidden(Hidden hidden) {
			this.hidden = hidden;
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

		public String getNullValue() {
			return this.nullValue;
		}

		public void setNullValue(String nullValue) {
			this.nullValue = nullValue;
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
