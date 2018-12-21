/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootContextLoader}
 *
 * @author Stephane Nicoll
 */
public class SpringBootContextLoaderTests {

	@Test
	public void environmentPropertiesSimple() {
		Map<String, Object> config = getEnvironmentProperties(SimpleConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	public void environmentPropertiesSimpleNonAlias() {
		Map<String, Object> config = getEnvironmentProperties(SimpleConfigNonAlias.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	public void environmentPropertiesOverrideDefaults() {
		Map<String, Object> config = getEnvironmentProperties(OverrideConfig.class);
		assertKey(config, "server.port", "2345");
	}

	@Test
	public void environmentPropertiesAppend() {
		Map<String, Object> config = getEnvironmentProperties(AppendConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "otherKey", "otherValue");
	}

	@Test
	public void environmentPropertiesSeparatorInValue() {
		Map<String, Object> config = getEnvironmentProperties(SameSeparatorInValue.class);
		assertKey(config, "key", "my=Value");
		assertKey(config, "anotherKey", "another:Value");
	}

	@Test
	public void environmentPropertiesAnotherSeparatorInValue() {
		Map<String, Object> config = getEnvironmentProperties(
				AnotherSeparatorInValue.class);
		assertKey(config, "key", "my:Value");
		assertKey(config, "anotherKey", "another=Value");
	}

	@Test
	@Ignore
	public void environmentPropertiesNewLineInValue() {
		// gh-4384
		Map<String, Object> config = getEnvironmentProperties(NewLineInValue.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "variables", "foo=FOO\n bar=BAR");
	}

	private Map<String, Object> getEnvironmentProperties(Class<?> testClass) {
		TestContext context = new ExposedTestContextManager(testClass)
				.getExposedTestContext();
		MergedContextConfiguration config = (MergedContextConfiguration) ReflectionTestUtils
				.getField(context, "mergedContextConfiguration");
		return TestPropertySourceUtils
				.convertInlinedPropertiesToMap(config.getPropertySourceProperties());
	}

	private void assertKey(Map<String, Object> actual, String key, Object value) {
		assertThat(actual.containsKey(key)).as("Key '" + key + "' not found").isTrue();
		assertThat(actual.get(key)).isEqualTo(value);
	}

	@SpringBootTest({ "key=myValue", "anotherKey:anotherValue" })
	@ContextConfiguration(classes = Config.class)
	static class SimpleConfig {

	}

	@SpringBootTest(properties = { "key=myValue", "anotherKey:anotherValue" })
	@ContextConfiguration(classes = Config.class)
	static class SimpleConfigNonAlias {

	}

	@SpringBootTest("server.port=2345")
	@ContextConfiguration(classes = Config.class)
	static class OverrideConfig {

	}

	@SpringBootTest({ "key=myValue", "otherKey=otherValue" })
	@ContextConfiguration(classes = Config.class)
	static class AppendConfig {

	}

	@SpringBootTest({ "key=my=Value", "anotherKey:another:Value" })
	@ContextConfiguration(classes = Config.class)
	static class SameSeparatorInValue {

	}

	@SpringBootTest({ "key=my:Value", "anotherKey:another=Value" })
	@ContextConfiguration(classes = Config.class)
	static class AnotherSeparatorInValue {

	}

	@SpringBootTest({ "key=myValue", "variables=foo=FOO\n bar=BAR" })
	@ContextConfiguration(classes = Config.class)
	static class NewLineInValue {

	}

	@Configuration
	static class Config {

	}

	/**
	 * {@link TestContextManager} which exposes the {@link TestContext}.
	 */
	private static class ExposedTestContextManager extends TestContextManager {

		ExposedTestContextManager(Class<?> testClass) {
			super(testClass);
		}

		public final TestContext getExposedTestContext() {
			return super.getTestContext();
		}

	}

}
