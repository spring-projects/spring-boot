/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link SpringApplicationContextLoader}
 *
 * @author Stephane Nicoll
 */
public class SpringApplicationContextLoaderTests {

	private final SpringApplicationContextLoader loader = new SpringApplicationContextLoader();

	@Test
	public void environmentPropertiesSimple() throws Exception {
		Map<String, Object> config = getEnvironmentProperties(SimpleConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	public void environmentPropertiesDefaults() throws Exception {
		Map<String, Object> config = getEnvironmentProperties(SimpleConfig.class);
		assertMissingKey(config, "server.port");
		assertKey(config, "spring.jmx.enabled", "false");
	}

	@Test
	public void environmentPropertiesOverrideDefaults() throws Exception {
		Map<String, Object> config = getEnvironmentProperties(OverrideConfig.class);
		assertKey(config, "server.port", "2345");
	}

	@Test(expected=IllegalStateException.class)
	public void environmentPropertiesIllegal() throws Exception {
		getEnvironmentProperties(IllegalConfig.class);
	}

	@Test
	public void environmentPropertiesAppend() throws Exception {
		Map<String, Object> config = getEnvironmentProperties(AppendConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "otherKey", "otherValue");
	}

	@Test
	public void environmentPropertiesSeparatorInValue() throws Exception {
		Map<String, Object> config = getEnvironmentProperties(SameSeparatorInValue.class);
		assertKey(config, "key", "my=Value");
		assertKey(config, "anotherKey", "another:Value");
	}

	@Test
	public void environmentPropertiesAnotherSeparatorInValue() throws Exception {
		Map<String, Object> config = getEnvironmentProperties(AnotherSeparatorInValue.class);
		assertKey(config, "key", "my:Value");
		assertKey(config, "anotherKey", "another=Value");
	}

	private Map<String, Object> getEnvironmentProperties(Class<?> testClass) throws Exception {
		TestContext context = new ExposedTestContextManager(testClass).getExposedTestContext();
		new IntegrationTestPropertiesListener().prepareTestInstance(context);
		MergedContextConfiguration config = (MergedContextConfiguration) ReflectionTestUtils.getField(
				context, "mergedContextConfiguration");
		return this.loader.extractEnvironmentProperties(config.getPropertySourceProperties());
	}

	private void assertKey(Map<String, Object> actual, String key, Object value) {
		assertTrue("Key '" + key + "' not found", actual.containsKey(key));
		assertEquals(value, actual.get(key));
	}

	private void assertMissingKey(Map<String, Object> actual, String key) {
		assertTrue("Key '" + key + "' found", !actual.containsKey(key));
	}

	@IntegrationTest({ "key=myValue", "anotherKey:anotherValue" })
	static class SimpleConfig {
	}

	@IntegrationTest({ "server.port=2345" })
	static class OverrideConfig {
	}

	@IntegrationTest(value = { "key=aValue", "anotherKey:anotherValue" }, properties = { "key=myValue", "otherKey=otherValue" })
	static class IllegalConfig {
	}

	@IntegrationTest(properties = { "key=myValue", "otherKey=otherValue" })
	static class AppendConfig {
	}

	@IntegrationTest({ "key=my=Value", "anotherKey:another:Value" })
	static class SameSeparatorInValue {
	}

	@IntegrationTest({ "key=my:Value", "anotherKey:another=Value" })
	static class AnotherSeparatorInValue {
	}
	
	private static class ExposedTestContextManager extends TestContextManager {

		public ExposedTestContextManager(Class<?> testClass) {
			super(testClass);
		}
		
		public final TestContext getExposedTestContext() {
			return super.getTestContext();
		}
		
		
	}

}
