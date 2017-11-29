/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.json;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.AssertProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObjectContentAssert}.
 *
 * @author Phillip Webb
 */
public class ObjectContentAssertTests {

	private static final ExampleObject SOURCE = new ExampleObject();

	private static final ExampleObject DIFFERENT;

	static {
		DIFFERENT = new ExampleObject();
		DIFFERENT.setAge(123);
	}

	@Test
	public void isEqualToWhenObjectsAreEqualShouldPass() throws Exception {
		assertThat(forObject(SOURCE)).isEqualTo(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void isEqualToWhenObjectsAreDifferentShouldFail() throws Exception {
		assertThat(forObject(SOURCE)).isEqualTo(DIFFERENT);
	}

	@Test
	public void asArrayForArrayShouldReturnObjectArrayAssert() throws Exception {
		ExampleObject[] source = new ExampleObject[] { SOURCE };
		assertThat(forObject(source)).asArray().containsExactly(SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void asArrayForNonArrayShouldFail() throws Exception {
		assertThat(forObject(SOURCE)).asArray();
	}

	@Test
	public void asMapForMapShouldReturnMapAssert() throws Exception {
		Map<String, ExampleObject> source = Collections.singletonMap("a", SOURCE);
		assertThat(forObject(source)).asMap().containsEntry("a", SOURCE);
	}

	@Test(expected = AssertionError.class)
	public void asMapForNonMapShouldFail() throws Exception {
		assertThat(forObject(SOURCE)).asMap();
	}

	private AssertProvider<ObjectContentAssert<Object>> forObject(final Object source) {
		return () -> new ObjectContentAssert<>(source);
	}

}
