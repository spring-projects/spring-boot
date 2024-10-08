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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointAccessOperationFilter}.
 *
 * @author Andy Wilkinson
 */
class EndpointAccessOperationFilterTests {

	private final EndpointAccessResolver accessResolver = mock(EndpointAccessResolver.class);

	private final Operation operation = mock(Operation.class);

	private final EndpointAccessOperationFilter<Operation> filter = new EndpointAccessOperationFilter<>(
			this.accessResolver);

	@Test
	void whenAccessIsUnrestrictedThenMatchReturnsTrue() {
		EndpointId endpointId = EndpointId.of("test");
		Access defaultAccess = Access.READ_ONLY;
		given(this.accessResolver.accessFor(endpointId, defaultAccess)).willReturn(Access.UNRESTRICTED);
		assertThat(this.filter.match(this.operation, endpointId, defaultAccess)).isTrue();
	}

	@Test
	void whenAccessIsNoneThenMatchReturnsFalse() {
		EndpointId endpointId = EndpointId.of("test");
		Access defaultAccess = Access.READ_ONLY;
		given(this.accessResolver.accessFor(endpointId, defaultAccess)).willReturn(Access.NONE);
		assertThat(this.filter.match(this.operation, endpointId, defaultAccess)).isFalse();
	}

	@Test
	void whenAccessIsReadOnlyAndOperationTypeIsReadThenMatchReturnsTrue() {
		EndpointId endpointId = EndpointId.of("test");
		Access defaultAccess = Access.READ_ONLY;
		given(this.accessResolver.accessFor(endpointId, defaultAccess)).willReturn(Access.READ_ONLY);
		given(this.operation.getType()).willReturn(OperationType.READ);
		assertThat(this.filter.match(this.operation, endpointId, defaultAccess)).isTrue();
	}

	@Test
	void whenAccessIsReadOnlyAndOperationTypeIsWriteThenMatchReturnsFalse() {
		EndpointId endpointId = EndpointId.of("test");
		Access defaultAccess = Access.READ_ONLY;
		given(this.accessResolver.accessFor(endpointId, defaultAccess)).willReturn(Access.READ_ONLY);
		given(this.operation.getType()).willReturn(OperationType.WRITE);
		assertThat(this.filter.match(this.operation, endpointId, defaultAccess)).isFalse();
	}

	@Test
	void whenAccessIsReadOnlyAndOperationTypeIsDeleteThenMatchReturnsFalse() {
		EndpointId endpointId = EndpointId.of("test");
		Access defaultAccess = Access.READ_ONLY;
		given(this.accessResolver.accessFor(endpointId, defaultAccess)).willReturn(Access.READ_ONLY);
		given(this.operation.getType()).willReturn(OperationType.DELETE);
		assertThat(this.filter.match(this.operation, endpointId, defaultAccess)).isFalse();
	}

}
