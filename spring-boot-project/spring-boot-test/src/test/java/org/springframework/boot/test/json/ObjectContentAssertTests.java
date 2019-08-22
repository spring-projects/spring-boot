/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.json;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ObjectContentAssert}.
 *
 * @author Phillip Webb
 */
class ObjectContentAssertTests {

	private static final ExampleObject SOURCE = new ExampleObject();

	private static final ExampleObject DIFFERENT;

	static {
		DIFFERENT = new ExampleObject();
		DIFFERENT.setAge(123);
	}

	@Test
	void isEqualToWhenObjectsAreEqualShouldPass() {
		assertThat(forObject(SOURCE)).isEqualTo(SOURCE);
	}

	@Test
	void isEqualToWhenObjectsAreDifferentShouldFail() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(forObject(SOURCE)).isEqualTo(DIFFERENT));
	}

	@Test
	void asArrayForArrayShouldReturnObjectArrayAssert() {
		ExampleObject[] source = new ExampleObject[] { SOURCE };
		assertThat(forObject(source)).asArray().containsExactly(SOURCE);
	}

	@Test
	void asArrayForNonArrayShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forObject(SOURCE)).asArray());
	}

	@Test
	void asMapForMapShouldReturnMapAssert() {
		Map<String, ExampleObject> source = Collections.singletonMap("a", SOURCE);
		assertThat(forObject(source)).asMap().containsEntry("a", SOURCE);
	}

	@Test
	void asMapForNonMapShouldFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forObject(SOURCE)).asMap());
	}

	private AssertProvider<ObjectContentAssert<Object>> forObject(Object source) {
		return () -> new ObjectContentAssert<>(source);
	}

}
