/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.loader.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemPropertyUtils}.
 *
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
		assertThat(SystemPropertyUtils.resolvePlaceholders("${foo}")).isEqualTo("bar");
	}

	@Test
	public void testDefaultValue() {
		assertThat(SystemPropertyUtils.resolvePlaceholders("${bar:foo}"))
				.isEqualTo("foo");
	}

	@Test
	public void testNestedPlaceholder() {
		assertThat(SystemPropertyUtils.resolvePlaceholders("${bar:${spam:foo}}"))
				.isEqualTo("foo");
	}

	@Test
	public void testEnvVar() {
		assertThat(SystemPropertyUtils.getProperty("lang"))
				.isEqualTo(System.getenv("LANG"));
	}

}
