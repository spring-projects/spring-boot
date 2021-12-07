/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
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
 * @author Scott Frederick
 * @author Madhura Bhave
 */
class SpringBootContextLoaderTests {

	@Test
	void environmentPropertiesSimple() {
		Map<String, Object> config = getMergedContextConfigurationProperties(SimpleConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	void environmentPropertiesSimpleNonAlias() {
		Map<String, Object> config = getMergedContextConfigurationProperties(SimpleConfigNonAlias.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	void environmentPropertiesOverrideDefaults() {
		Map<String, Object> config = getMergedContextConfigurationProperties(OverrideConfig.class);
		assertKey(config, "server.port", "2345");
	}

	@Test
	void environmentPropertiesAppend() {
		Map<String, Object> config = getMergedContextConfigurationProperties(AppendConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "otherKey", "otherValue");
	}

	@Test
	void environmentPropertiesSeparatorInValue() {
		Map<String, Object> config = getMergedContextConfigurationProperties(SameSeparatorInValue.class);
		assertKey(config, "key", "my=Value");
		assertKey(config, "anotherKey", "another:Value");
	}

	@Test
	void environmentPropertiesAnotherSeparatorInValue() {
		Map<String, Object> config = getMergedContextConfigurationProperties(AnotherSeparatorInValue.class);
		assertKey(config, "key", "my:Value");
		assertKey(config, "anotherKey", "another=Value");
	}

	@Test // gh-4384
	@Disabled
	void environmentPropertiesNewLineInValue() {
		Map<String, Object> config = getMergedContextConfigurationProperties(NewLineInValue.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "variables", "foo=FOO\n bar=BAR");
	}

	@Test
	void noActiveProfiles() {
		assertThat(getActiveProfiles(SimpleConfig.class)).isEmpty();
	}

	@Test
	void multipleActiveProfiles() {
		assertThat(getActiveProfiles(MultipleActiveProfiles.class)).containsExactly("profile1", "profile2");
	}

	@Test
	void activeProfileWithComma() {
		assertThat(getActiveProfiles(ActiveProfileWithComma.class)).containsExactly("profile1,2");
	}

	@Test
	// gh-28776
	void testPropertyValuesShouldTakePrecedenceWhenInlinedPropertiesPresent() {
		TestContext context = new ExposedTestContextManager(SimpleConfig.class).getExposedTestContext();
		StandardEnvironment environment = (StandardEnvironment) context.getApplicationContext().getEnvironment();
		TestPropertyValues.of("key=thisValue").applyTo(environment);
		assertThat(environment.getProperty("key")).isEqualTo("thisValue");
		assertThat(environment.getPropertySources().get("active-test-profiles")).isNull();
	}

	@Test
	void testPropertyValuesShouldTakePrecedenceWhenInlinedPropertiesPresentAndProfilesActive() {
		TestContext context = new ExposedTestContextManager(ActiveProfileWithInlinedProperties.class)
				.getExposedTestContext();
		StandardEnvironment environment = (StandardEnvironment) context.getApplicationContext().getEnvironment();
		TestPropertyValues.of("key=thisValue").applyTo(environment);
		assertThat(environment.getProperty("key")).isEqualTo("thisValue");
		assertThat(environment.getPropertySources().get("active-test-profiles")).isNotNull();
	}

	private String[] getActiveProfiles(Class<?> testClass) {
		TestContext testContext = new ExposedTestContextManager(testClass).getExposedTestContext();
		ApplicationContext applicationContext = testContext.getApplicationContext();
		return applicationContext.getEnvironment().getActiveProfiles();
	}

	private Map<String, Object> getMergedContextConfigurationProperties(Class<?> testClass) {
		TestContext context = new ExposedTestContextManager(testClass).getExposedTestContext();
		MergedContextConfiguration config = (MergedContextConfiguration) ReflectionTestUtils.getField(context,
				"mergedContextConfiguration");
		return TestPropertySourceUtils.convertInlinedPropertiesToMap(config.getPropertySourceProperties());
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

	@SpringBootTest
	@ActiveProfiles({ "profile1", "profile2" })
	@ContextConfiguration(classes = Config.class)
	static class MultipleActiveProfiles {

	}

	@SpringBootTest
	@ActiveProfiles({ "profile1,2" })
	@ContextConfiguration(classes = Config.class)
	static class ActiveProfileWithComma {

	}

	@SpringBootTest({ "key=myValue" })
	@ActiveProfiles({ "profile1,2" })
	@ContextConfiguration(classes = Config.class)
	static class ActiveProfileWithInlinedProperties {

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

	/**
	 * {@link TestContextManager} which exposes the {@link TestContext}.
	 */
	static class ExposedTestContextManager extends TestContextManager {

		ExposedTestContextManager(Class<?> testClass) {
			super(testClass);
		}

		final TestContext getExposedTestContext() {
			return super.getTestContext();
		}

	}

}
