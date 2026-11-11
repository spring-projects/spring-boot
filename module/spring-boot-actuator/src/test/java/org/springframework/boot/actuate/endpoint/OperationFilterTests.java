/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OperationFilter}.
 *
 * @author Andy Wilkinson
 */
class OperationFilterTests {

	private final EndpointAccessResolver accessResolver = mock(EndpointAccessResolver.class);

	private final Operation operation = mock(Operation.class);

	private final OperationFilter<Operation> filter = OperationFilter.byAccess(this.accessResolver);

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
