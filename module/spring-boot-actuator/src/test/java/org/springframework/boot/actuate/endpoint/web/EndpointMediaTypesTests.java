/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link EndpointMediaTypes}.
 *
 * @author Phillip Webb
 */
class EndpointMediaTypesTests {

	private static final String V2_JSON = ApiVersion.V2.getProducedMimeType().toString();

	private static final String V3_JSON = ApiVersion.V3.getProducedMimeType().toString();

	@Test
	void defaultReturnsExpectedProducedAndConsumedTypes() {
		assertThat(EndpointMediaTypes.DEFAULT.getProduced()).containsExactly(V3_JSON, V2_JSON, "application/json");
		assertThat(EndpointMediaTypes.DEFAULT.getConsumed()).containsExactly(V3_JSON, V2_JSON, "application/json");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenProducedIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EndpointMediaTypes(null, Collections.emptyList()))
			.withMessageContaining("'produced' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenConsumedIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EndpointMediaTypes(Collections.emptyList(), null))
			.withMessageContaining("'consumed' must not be null");
	}

	@Test
	void createFromProducedAndConsumedUsesSameListForBoth() {
		EndpointMediaTypes types = new EndpointMediaTypes("spring/framework", "spring/boot");
		assertThat(types.getProduced()).containsExactly("spring/framework", "spring/boot");
		assertThat(types.getConsumed()).containsExactly("spring/framework", "spring/boot");
	}

	@Test
	void getProducedShouldReturnProduced() {
		List<String> produced = Arrays.asList("a", "b", "c");
		EndpointMediaTypes types = new EndpointMediaTypes(produced, Collections.emptyList());
		assertThat(types.getProduced()).isEqualTo(produced);
	}

	@Test
	void getConsumedShouldReturnConsumed() {
		List<String> consumed = Arrays.asList("a", "b", "c");
		EndpointMediaTypes types = new EndpointMediaTypes(Collections.emptyList(), consumed);
		assertThat(types.getConsumed()).isEqualTo(consumed);
	}

}
