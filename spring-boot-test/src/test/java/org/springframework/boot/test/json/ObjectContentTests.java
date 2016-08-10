/*
 * Copyright 2012-2016 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObjectContent}.
 *
 * @author Phillip Webb
 */
public class ObjectContentTests {

	private static final ExampleObject OBJECT = new ExampleObject();

	private static final ResolvableType TYPE = ResolvableType
			.forClass(ExampleObject.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenObjectIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Object must not be null");
		new ObjectContent<ExampleObject>(TYPE, null);
	}

	@Test
	public void createWhenTypeIsNullShouldCreateContent() throws Exception {
		ObjectContent<ExampleObject> content = new ObjectContent<ExampleObject>(null,
				OBJECT);
		assertThat(content).isNotNull();
	}

	@Test
	public void assertThatShouldReturnObjectContentAssert() throws Exception {
		ObjectContent<ExampleObject> content = new ObjectContent<ExampleObject>(TYPE,
				OBJECT);
		assertThat(content.assertThat()).isInstanceOf(ObjectContentAssert.class);
	}

	@Test
	public void getObjectShouldReturnObject() throws Exception {
		ObjectContent<ExampleObject> content = new ObjectContent<ExampleObject>(TYPE,
				OBJECT);
		assertThat(content.getObject()).isEqualTo(OBJECT);
	}

	@Test
	public void toStringWhenHasTypeShouldReturnString() throws Exception {
		ObjectContent<ExampleObject> content = new ObjectContent<ExampleObject>(TYPE,
				OBJECT);
		assertThat(content.toString())
				.isEqualTo("ObjectContent " + OBJECT + " created from " + TYPE);
	}

	@Test
	public void toStringWhenHasNoTypeShouldReturnString() throws Exception {
		ObjectContent<ExampleObject> content = new ObjectContent<ExampleObject>(null,
				OBJECT);
		assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT);
	}

}
