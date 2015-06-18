/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EnvironmentTestUtils}.
 *
 * @author Stephane Nicoll
 */
public class EnvironmentTestUtilsTests {

	private final ConfigurableEnvironment environment = new StandardEnvironment();

	@Test
	public void addSimplePairEqual() {
		testAddSimplePair("my.foo", "bar", "=");
	}

	@Test
	public void addSimplePairColon() {
		testAddSimplePair("my.foo", "bar", ":");
	}

	@Test
	public void addSimplePairEqualWithEqualInValue() {
		testAddSimplePair("my.foo", "b=ar", "=");
	}

	@Test
	public void addSimplePairEqualWithColonInValue() {
		testAddSimplePair("my.foo", "b:ar", "=");
	}

	@Test
	public void addSimplePairColonWithColonInValue() {
		testAddSimplePair("my.foo", "b:ar", ":");
	}

	@Test
	public void addSimplePairColonWithEqualInValue() {
		testAddSimplePair("my.foo", "b=ar", ":");
	}

	@Test
	public void addPairNoValue() {
		String propertyName = "my.foo+bar";
		assertFalse(environment.containsProperty(propertyName));
		EnvironmentTestUtils.addEnvironment(environment, propertyName);
		assertTrue(environment.containsProperty(propertyName));
		assertEquals("", environment.getProperty(propertyName));
	}

	private void testAddSimplePair(String key, String value, String delimiter) {
		assertFalse("Property '" + key + "' should not exist", environment.containsProperty(key));
		EnvironmentTestUtils.addEnvironment(environment, key + delimiter + value);
		assertEquals("Wrong value for property '" + key + "'", value, environment.getProperty(key));
	}

	@Test
	public void testConfigHasHigherPrecedence() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("my.foo", "bar");
		MapPropertySource source = new MapPropertySource("sample", map);
		environment.getPropertySources().addFirst(source);
		assertEquals("bar", environment.getProperty("my.foo"));
		EnvironmentTestUtils.addEnvironment(environment, "my.foo=bar2");
		assertEquals("bar2", environment.getProperty("my.foo"));
	}

}
