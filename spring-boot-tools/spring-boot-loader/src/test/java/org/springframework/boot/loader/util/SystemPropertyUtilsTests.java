/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.loader.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 */
public class SystemPropertyUtilsTests {

	@BeforeClass
	public static void init() {
		System.setProperty("foo", "bar");
	}

	@AfterClass
	public static void close() {
		System.clearProperty("foo");
	}

	@Test
	public void testVanillaPlaceholder() {
		assertEquals("bar", SystemPropertyUtils.resolvePlaceholders("${foo}"));
	}

	@Test
	public void testDefaultValue() {
		assertEquals("foo", SystemPropertyUtils.resolvePlaceholders("${bar:foo}"));
	}

	@Test
	public void testNestedPlaceholder() {
		assertEquals("foo",
				SystemPropertyUtils.resolvePlaceholders("${bar:${spam:foo}}"));
	}

	@Test
	public void testEnvVar() {
		assertEquals(System.getenv("LANG"), SystemPropertyUtils.getProperty("lang"));
	}

}
