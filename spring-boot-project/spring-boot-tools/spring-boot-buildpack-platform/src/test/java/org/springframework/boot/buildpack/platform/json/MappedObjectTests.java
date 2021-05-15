/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.MappedObjectTests.TestMappedObject.Person;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappedObject}.
 *
 * @author Phillip Webb
 */
class MappedObjectTests extends AbstractJsonTests {

	private final TestMappedObject mapped;

	MappedObjectTests() throws IOException {
		this.mapped = TestMappedObject.of(getContent("test-mapped-object.json"));
	}

	@Test
	void ofReadsJson() throws Exception {
		assertThat(this.mapped.getNode()).isNotNull();
	}

	@Test
	void valueAtWhenStringReturnsValue() {
		assertThat(this.mapped.valueAt("/string", String.class)).isEqualTo("stringvalue");
	}

	@Test
	void valueAtWhenStringArrayReturnsValue() {
		assertThat(this.mapped.valueAt("/stringarray", String[].class)).containsExactly("a", "b");
	}

	@Test
	void valueAtWhenMissingReturnsNull() {
		assertThat(this.mapped.valueAt("/missing", String.class)).isNull();
	}

	@Test
	void valueAtWhenInterfaceReturnsProxy() {
		Person person = this.mapped.valueAt("/person", Person.class);
		assertThat(person.getName().getFirst()).isEqualTo("spring");
		assertThat(person.getName().getLast()).isEqualTo("boot");
	}

	@Test
	void valueAtWhenInterfaceAndMissingReturnsProxy() {
		Person person = this.mapped.valueAt("/missing", Person.class);
		assertThat(person.getName().getFirst()).isNull();
		assertThat(person.getName().getLast()).isNull();
	}

	@Test
	void valueAtWhenActualPropertyStartsWithUppercaseReturnsValue() {
		assertThat(this.mapped.valueAt("/startsWithUppercase", String.class)).isEqualTo("value");
	}

	@Test
	void valueAtWhenDefaultMethodReturnsValue() {
		Person person = this.mapped.valueAt("/person", Person.class);
		assertThat(person.getName().getFullName()).isEqualTo("dr spring boot");
	}

	/**
	 * {@link MappedObject} for testing.
	 */
	static class TestMappedObject extends MappedObject {

		TestMappedObject(JsonNode node) {
			super(node, MethodHandles.lookup());
		}

		static TestMappedObject of(InputStream content) throws IOException {
			return of(content, TestMappedObject::new);
		}

		interface Person {

			Name getName();

			interface Name {

				String getFirst();

				String getLast();

				default String getFullName() {
					String title = valueAt(this, "/title", String.class);
					return title + " " + getFirst() + " " + getLast();
				}

			}

		}

	}

}
