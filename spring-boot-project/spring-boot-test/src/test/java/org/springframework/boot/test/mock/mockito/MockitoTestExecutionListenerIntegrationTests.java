/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.test.mock.mockito;

import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoTestExecutionListener}.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(SpringExtension.class)
class MockitoTestExecutionListenerIntegrationTests {

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class DisabledTests {

		private static final UUID uuid = UUID.randomUUID();

		@Mock
		private MockedStatic<UUID> mockedStatic;

		@Test
		@Order(1)
		@Disabled
		void shouldReturnConstantValueDisabled() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

		@Test
		@Order(2)
		void shouldReturnConstantValue() {
			this.mockedStatic.when(UUID::randomUUID).thenReturn(uuid);
			UUID result = UUID.randomUUID();
			assertThat(result).isEqualTo(uuid);
		}

	}

}
