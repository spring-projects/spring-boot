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

package org.springframework.boot.test.mock.mockito;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.mock.MockCreationSettings;

import org.springframework.boot.test.mock.mockito.example.ExampleExtraInterface;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MockDefinition}.
 *
 * @author Phillip Webb
 */
public class MockDefinitionTests {

	private static final ResolvableType EXAMPLE_SERVICE_TYPE = ResolvableType
			.forClass(ExampleService.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void classToMockMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("TypeToMock must not be null");
		new MockDefinition(null, null, null, null, false, null, null);
	}

	@Test
	public void createWithDefaults() throws Exception {
		MockDefinition definition = new MockDefinition(null, EXAMPLE_SERVICE_TYPE, null,
				null, false, null, null);
		assertThat(definition.getName()).isNull();
		assertThat(definition.getTypeToMock()).isEqualTo(EXAMPLE_SERVICE_TYPE);
		assertThat(definition.getExtraInterfaces()).isEmpty();
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_DEFAULTS);
		assertThat(definition.isSerializable()).isFalse();
		assertThat(definition.getReset()).isEqualTo(MockReset.AFTER);
		assertThat(definition.getQualifier()).isNull();
	}

	@Test
	public void createExplicit() throws Exception {
		QualifierDefinition qualifier = mock(QualifierDefinition.class);
		MockDefinition definition = new MockDefinition("name", EXAMPLE_SERVICE_TYPE,
				new Class<?>[] { ExampleExtraInterface.class },
				Answers.RETURNS_SMART_NULLS, true, MockReset.BEFORE, qualifier);
		assertThat(definition.getName()).isEqualTo("name");
		assertThat(definition.getTypeToMock()).isEqualTo(EXAMPLE_SERVICE_TYPE);
		assertThat(definition.getExtraInterfaces())
				.containsExactly(ExampleExtraInterface.class);
		assertThat(definition.getAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
		assertThat(definition.isSerializable()).isTrue();
		assertThat(definition.getReset()).isEqualTo(MockReset.BEFORE);
		assertThat(definition.isProxyTargetAware()).isFalse();
		assertThat(definition.getQualifier()).isEqualTo(qualifier);
	}

	@Test
	public void createMock() throws Exception {
		MockDefinition definition = new MockDefinition("name", EXAMPLE_SERVICE_TYPE,
				new Class<?>[] { ExampleExtraInterface.class },
				Answers.RETURNS_SMART_NULLS, true, MockReset.BEFORE, null);
		ExampleService mock = definition.createMock();
		MockCreationSettings<?> settings = MockitoApi.get().getMockSettings(mock);
		assertThat(mock).isInstanceOf(ExampleService.class);
		assertThat(mock).isInstanceOf(ExampleExtraInterface.class);
		assertThat(settings.getMockName().toString()).isEqualTo("name");
		assertThat(settings.getDefaultAnswer()).isEqualTo(Answers.RETURNS_SMART_NULLS);
		assertThat(settings.isSerializable()).isTrue();
		assertThat(MockReset.get(mock)).isEqualTo(MockReset.BEFORE);
	}

}
