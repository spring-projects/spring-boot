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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.junit.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConfigurationPropertiesReportEndpointTests extends
		AbstractEndpointTests<ConfigurationPropertiesReportEndpoint> {

	public ConfigurationPropertiesReportEndpointTests() {
		super(Config.class, ConfigurationPropertiesReportEndpoint.class, "/configprops",
				true, "endpoints.configprops");
	}

	@Test
	public void testInvoke() throws Exception {
		assertThat(getEndpointBean().invoke().size(), greaterThan(0));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDefaultKeySanitization() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		// report.setKeysToSanitize(new String[] {});
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("testProperties");
		assertNotNull(nestedProperties);
		assertEquals("******", nestedProperties.get("dbPassword"));
		assertEquals("654321", nestedProperties.get("myTestProperty"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKeySanitization() throws Exception {
		ConfigurationPropertiesReportEndpoint report = getEndpointBean();
		report.setKeysToSanitize(new String[] { "property" });
		Map<String, Object> properties = report.invoke();
		Map<String, Object> nestedProperties = (Map<String, Object>) properties
				.get("testProperties");
		assertNotNull(nestedProperties);
		assertEquals("123456", nestedProperties.get("dbPassword"));
		assertEquals("******", nestedProperties.get("myTestProperty"));
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

		@ConfigurationProperties(name = "test")
		public static class TestProperties {

			private String dbPassword = "123456";

			private String myTestProperty = "654321";

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

		}
	}
}
