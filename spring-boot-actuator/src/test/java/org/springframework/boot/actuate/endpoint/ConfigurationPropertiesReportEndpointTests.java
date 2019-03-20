/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Dave Syer
 */
public class ConfigurationPropertiesReportEndpointTests
		extends AbstractEndpointTests<ConfigurationPropertiesReportEndpoint> {

	public ConfigurationPropertiesReportEndpointTests() {
		super(Config.class, ConfigurationPropertiesReportEndpoint.class, "configprops",
				true, "endpoints.configprops");
	}

	@Test
	public void testInvoke() throws Exception {
		assertThat(getEndpointBean().invoke().size()).isGreaterThan(0);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNaming() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("testProperties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("prefix")).isEqualTo("test");
		assertThat(nestedProperties.get("properties")).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDefaultKeySanitization() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("******");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("654321");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKeySanitization() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		report.setKeysToSanitize("property");
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("123456");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("******");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKeySanitizationWithList() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		report.setKeysToSanitize("property");
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("123456");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("******");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomPattern() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		report.setKeysToSanitize(".*pass.*");
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("******");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("654321");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomKeysByEnvironment() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.configprops.keys-to-sanitize:property");
		this.context.register(Config.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("123456");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("******");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationWithCustomPatternByEnvironment() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.configprops.keys-to-sanitize: .*pass.*");
		this.context.register(Config.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("******");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("654321");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKeySanitizationWithCustomPatternAndKeyByEnvironment()
			throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.configprops.keys-to-sanitize: .*pass.*, property");
		this.context.register(Config.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		assertThat(nestedProperties.get("dbPassword")).isEqualTo("******");
		assertThat(nestedProperties.get("myTestProperty")).isEqualTo("******");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKeySanitizationWithCustomPatternUsingCompositeKeys()
			throws Exception {
		// gh-4415
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.configprops.keys-to-sanitize: .*\\.secrets\\..*, .*\\.hidden\\..*");
		this.context.register(Config.class);
		this.context.refresh();
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties).isNotNull();
		Map<String, Object> secrets = (Map<String, Object>) nestedProperties
				.get("secrets");
		Map<String, Object> hidden = (Map<String, Object>) nestedProperties.get("hidden");
		assertThat(secrets.get("mine")).isEqualTo("******");
		assertThat(secrets.get("yours")).isEqualTo("******");
		assertThat(hidden.get("mine")).isEqualTo("******");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void mixedBoolean() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties.get("mixedBoolean")).isEqualTo(true);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listsAreSanitized() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties.get("listItems")).isInstanceOf(List.class);
		List<Object> list = (List<Object>) nestedProperties.get("listItems");
		assertThat(list).hasSize(1);
		Map<String, Object> item = (Map<String, Object>) list.get(0);
		assertThat(item.get("somePassword")).isEqualTo("******");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void listsOfListsAreSanitized() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) ((Map<String, Object>) properties
				.get("testProperties")).get("properties");
		assertThat(nestedProperties.get("listOfListItems")).isInstanceOf(List.class);
		List<List<Object>> listOfLists = (List<List<Object>>) nestedProperties
				.get("listOfListItems");
		assertThat(listOfLists).hasSize(1);
		List<Object> list = listOfLists.get(0);
		assertThat(list).hasSize(1);
		Map<String, Object> item = (Map<String, Object>) list.get(0);
		assertThat(item.get("somePassword")).isEqualTo("******");
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

		private Map<String, Object> secrets = new HashMap<String, Object>();

		private Hidden hidden = new Hidden();

		private List<ListItem> listItems = new ArrayList<ListItem>();

		private List<List<ListItem>> listOfListItems = new ArrayList<List<ListItem>>();

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
			return (this.mixedBoolean != null) ? this.mixedBoolean : false;
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
