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

package org.springframework.boot.test.json;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ObjectContent}.
 *
 * @author Phillip Webb
 */
class ObjectContentTests {

	private static final ExampleObject OBJECT = new ExampleObject();

	private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenObjectIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ObjectContent<ExampleObject>(TYPE, null))
			.withMessageContaining("'object' must not be null");
	}

	@Test
	void createWhenTypeIsNullShouldCreateContent() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(null, OBJECT);
		assertThat(content).isNotNull();
	}

	@Test
	void assertThatShouldReturnObjectContentAssert() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
		assertThat(content.assertThat()).isInstanceOf(ObjectContentAssert.class);
	}

	@Test
	void getObjectShouldReturnObject() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
		assertThat(content.getObject()).isEqualTo(OBJECT);
	}

	@Test
	void toStringWhenHasTypeShouldReturnString() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
		assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT + " created from " + TYPE);
	}

	@Test
	void toStringWhenHasNoTypeShouldReturnString() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(null, OBJECT);
		assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT);
	}

}
