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

package org.springframework.boot.test.mock.mockito;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.internal.util.MockUtil;
import org.mockito.mock.MockCreationSettings;

import org.springframework.boot.test.mock.mockito.example.ExampleExtraInterface;
import org.springframework.boot.test.mock.mockito.example.ExampleService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockDefinition}.
 *
 * @author Phillip Webb
 */
public class MockDefinitionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void ClassToMockMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ClassToMock must not be null");
		new MockDefinition(null, null, null, null, false, null, true);
	}

	@Test
	public void createWithDefaults() throws Exception {
		MockDefinition definition = new MockDefinition(null, ExampleService.class, null,
				null, false, null, true);
		assertThat(definition.getName()).isNull();
		assertThat(definition.getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(definition.getExtraInterfaces()).isEmpty();
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_DEFAULTS);
		assertThat(definition.isSerializable()).isFalse();
		assertThat(definition.getReset()).isEqualTo(MockReset.AFTER);
	}

	@Test
	public void createExplicit() throws Exception {
		MockDefinition definition = new MockDefinition("name", ExampleService.class,
				new Class<?>[] { ExampleExtraInterface.class },
				Answers.RETURNS_SMART_NULLS, true, MockReset.BEFORE, false);
		assertThat(definition.getName()).isEqualTo("name");
		assertThat(definition.getClassToMock()).isEqualTo(ExampleService.class);
		assertThat(definition.getExtraInterfaces())
				.containsExactly(ExampleExtraInterface.class);
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
		assertThat(definition.isSerializable()).isTrue();
		assertThat(definition.getReset()).isEqualTo(MockReset.BEFORE);
		assertThat(definition.isProxyTargetAware()).isFalse();
	}

	@Test
	public void createMock() throws Exception {
		MockDefinition definition = new MockDefinition("name", ExampleService.class,
				new Class<?>[] { ExampleExtraInterface.class },
				Answers.RETURNS_SMART_NULLS, true, MockReset.BEFORE, true);
		ExampleService mock = definition.createMock();
		MockCreationSettings<?> settings = new MockUtil().getMockSettings(mock);
		assertThat(mock).isInstanceOf(ExampleService.class);
		assertThat(mock).isInstanceOf(ExampleExtraInterface.class);
		assertThat(settings.getMockName().toString()).isEqualTo("name");
		assertThat(settings.getDefaultAnswer())
				.isEqualTo(Answers.RETURNS_SMART_NULLS.get());
		assertThat(settings.isSerializable()).isTrue();
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
	}

}
