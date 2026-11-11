/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link OperationResponseBody}.
 *
 * @author Phillip Webb
 */
class OperationResponseBodyTests {

	@Test
	void ofMapReturnsOperationResponseBody() {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put("one", "1");
		map.put("two", "2");
		Map<String, String> mapDescriptor = OperationResponseBody.of(map);
		assertThat(mapDescriptor).containsExactly(entry("one", "1"), entry("two", "2"));
		assertThat(mapDescriptor).isInstanceOf(OperationResponseBody.class);
	}

}
