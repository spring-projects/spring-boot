/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AutoConfigurationMetadataLoader}.
 *
 * @author Phillip Webb
 */
@WithResource(name = "metadata.properties", content = """
		test=
		test.string=abc
		test.int=123
		test.set=a,b,b,c
		""")
class AutoConfigurationMetadataLoaderTests {

	@Test
	void loadShouldLoadProperties() {
		assertThat(load()).isNotNull();
	}

	@Test
	void wasProcessedWhenProcessedShouldReturnTrue() {
		assertThat(load().wasProcessed("test")).isTrue();
	}

	@Test
	void wasProcessedWhenNotProcessedShouldReturnFalse() {
		assertThat(load().wasProcessed("testx")).isFalse();
	}

	@Test
	void getIntegerShouldReturnValue() {
		assertThat(load().getInteger("test", "int")).isEqualTo(123);
	}

	@Test
	void getIntegerWhenMissingShouldReturnNull() {
		assertThat(load().getInteger("test", "intx")).isNull();
	}

	@Test
	void getIntegerWithDefaultWhenMissingShouldReturnDefault() {
		assertThat(load().getInteger("test", "intx", 345)).isEqualTo(345);
	}

	@Test
	void getSetShouldReturnValue() {
		assertThat(load().getSet("test", "set")).containsExactly("a", "b", "c");
	}

	@Test
	void getSetWhenMissingShouldReturnNull() {
		assertThat(load().getSet("test", "setx")).isNull();
	}

	@Test
	void getSetWithDefaultWhenMissingShouldReturnDefault() {
		assertThat(load().getSet("test", "setx", Collections.singleton("x"))).containsExactly("x");
	}

	@Test
	void getShouldReturnValue() {
		assertThat(load().get("test", "string")).isEqualTo("abc");
	}

	@Test
	void getWhenMissingShouldReturnNull() {
		assertThat(load().get("test", "stringx")).isNull();
	}

	@Test
	void getWithDefaultWhenMissingShouldReturnDefault() {
		assertThat(load().get("test", "stringx", "xyz")).isEqualTo("xyz");
	}

	private AutoConfigurationMetadata load() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return AutoConfigurationMetadataLoader.loadMetadata(classLoader, "metadata.properties");
	}

}
