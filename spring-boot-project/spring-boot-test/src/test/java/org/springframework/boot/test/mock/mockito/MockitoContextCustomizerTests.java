/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockitoContextCustomizer}.
 *
 * @author Phillip Webb
 */
public class MockitoContextCustomizerTests {

	private static final Set<MockDefinition> NO_DEFINITIONS = Collections.emptySet();

	@Test
	public void hashCodeAndEquals() {
		MockDefinition d1 = createTestMockDefinition(ExampleService.class);
		MockDefinition d2 = createTestMockDefinition(ExampleServiceCaller.class);
		MockitoContextCustomizer c1 = new MockitoContextCustomizer(NO_DEFINITIONS);
		MockitoContextCustomizer c2 = new MockitoContextCustomizer(
				new LinkedHashSet<>(Arrays.asList(d1, d2)));
		MockitoContextCustomizer c3 = new MockitoContextCustomizer(
				new LinkedHashSet<>(Arrays.asList(d2, d1)));
		assertThat(c2.hashCode()).isEqualTo(c3.hashCode());
		assertThat(c1).isEqualTo(c1).isNotEqualTo(c2);
		assertThat(c2).isEqualTo(c2).isEqualTo(c3).isNotEqualTo(c1);
	}

	private MockDefinition createTestMockDefinition(Class<?> typeToMock) {
		return new MockDefinition(null, ResolvableType.forClass(typeToMock), null, null,
				false, null, null);
	}

}
