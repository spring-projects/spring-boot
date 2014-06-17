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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.Test;

import org.springframework.test.context.MergedContextConfiguration;

/**
 * Tests for {@link SpringApplicationContextLoader}
 *
 * @author Stephane Nicoll
 */
public class SpringApplicationContextLoaderTests {

	private final SpringApplicationContextLoader loader = new SpringApplicationContextLoader();

	@Test
	public void environmentPropertiesSimple() {
		Map<String, Object> config = getEnvironmentProperties(SimpleConfig.class);
		assertKey(config, "key", "myValue");
		assertKey(config, "anotherKey", "anotherValue");
	}

	@Test
	public void environmentPropertiesSeparatorInValue() {
		Map<String, Object> config = getEnvironmentProperties(SameSeparatorInValue.class);
		assertKey(config, "key", "my=Value");
		assertKey(config, "anotherKey", "another:Value");
	}

	@Test
	public void environmentPropertiesAnotherSeparatorInValue() {
		Map<String, Object> config = getEnvironmentProperties(AnotherSeparatorInValue.class);
		assertKey(config, "key", "my:Value");
		assertKey(config, "anotherKey", "another=Value");
	}


	private Map<String, Object> getEnvironmentProperties(Class<?> testClass) {
		MergedContextConfiguration configuration = mock(MergedContextConfiguration.class);
		doReturn(testClass).when(configuration).getTestClass();

		return loader.getEnvironmentProperties(configuration);
	}

	private void assertKey(Map<String, Object> actual, String key, Object value) {
		assertTrue("Key '" + key + "' not found", actual.containsKey(key));
		assertEquals(value, actual.get(key));
	}


	@IntegrationTest({"key=myValue", "anotherKey:anotherValue"})
	static class SimpleConfig {
	}

	@IntegrationTest({"key=my=Value", "anotherKey:another:Value"})
	static class SameSeparatorInValue {
	}

	@IntegrationTest({"key=my:Value", "anotherKey:another=Value"})
	static class AnotherSeparatorInValue {
	}

}
