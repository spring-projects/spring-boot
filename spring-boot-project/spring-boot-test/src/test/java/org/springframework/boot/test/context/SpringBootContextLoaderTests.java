/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.WebApplicationContext;

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

	@Test
	void propertySourceOrdering() throws Exception {
		TestContext context = new ExposedTestContextManager(PropertySourceOrdering.class).getExposedTestContext();
		ConfigurableEnvironment environment = (ConfigurableEnvironment) context.getApplicationContext()
				.getEnvironment();
		List<String> names = environment.getPropertySources().stream().map(PropertySource::getName)
				.collect(Collectors.toList());
		String last = names.remove(names.size() - 1);
		assertThat(names).containsExactly("configurationProperties", "commandLineArgs", "Inlined Test Properties",
				"servletConfigInitParams", "servletContextInitParams", "systemProperties", "systemEnvironment",
				"random");
		assertThat(last).startsWith("Config resource");
	}

	@Test
	void whenEnvironmentChangesWebApplicationTypeToNoneThenContextTypeChangesAccordingly() {
		TestContext context = new ExposedTestContextManager(ChangingWebApplicationTypeToNone.class)
				.getExposedTestContext();
		assertThat(context.getApplicationContext()).isNotInstanceOf(WebApplicationContext.class);
	}

	@Test
	void whenEnvironmentChangesWebApplicationTypeToReactiveThenContextTypeChangesAccordingly() {
		TestContext context = new ExposedTestContextManager(ChangingWebApplicationTypeToReactive.class)
				.getExposedTestContext();
		assertThat(context.getApplicationContext()).isInstanceOf(GenericReactiveWebApplicationContext.class);
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

	@SpringBootTest(properties = { "key=myValue", "anotherKey:anotherValue" }, classes = Config.class)
	static class SimpleConfig {

	}

	@SpringBootTest(properties = { "key=myValue", "anotherKey:anotherValue" }, classes = Config.class)
	static class SimpleConfigNonAlias {

	}

	@SpringBootTest(properties = "server.port=2345", classes = Config.class)
	static class OverrideConfig {

	}

	@SpringBootTest(properties = { "key=myValue", "otherKey=otherValue" }, classes = Config.class)
	static class AppendConfig {

	}

	@SpringBootTest(properties = { "key=my=Value", "anotherKey:another:Value" }, classes = Config.class)
	static class SameSeparatorInValue {

	}

	@SpringBootTest(properties = { "key=my:Value", "anotherKey:another=Value" }, classes = Config.class)
	static class AnotherSeparatorInValue {

	}

	@SpringBootTest(properties = { "key=myValue", "variables=foo=FOO\n bar=BAR" }, classes = Config.class)
	static class NewLineInValue {

	}

	@SpringBootTest(classes = Config.class)
	@ActiveProfiles({ "profile1", "profile2" })
	static class MultipleActiveProfiles {

	}

	@SpringBootTest(classes = Config.class)
	@ActiveProfiles({ "profile1,2" })
	static class ActiveProfileWithComma {

	}

	@SpringBootTest(properties = { "key=myValue" }, classes = Config.class)
	@ActiveProfiles({ "profile1,2" })
	static class ActiveProfileWithInlinedProperties {

	}

	@SpringBootTest(classes = Config.class, args = "args", properties = "one=1")
	@TestPropertySource(properties = "two=2")
	static class PropertySourceOrdering {

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

	@SpringBootTest(classes = Config.class, args = "--spring.main.web-application-type=none")
	static class ChangingWebApplicationTypeToNone {

	}

	@SpringBootTest(classes = Config.class, args = "--spring.main.web-application-type=reactive")
	static class ChangingWebApplicationTypeToReactive {

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
