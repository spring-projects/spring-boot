/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilteredHostException}.
 *
 * @author Phillip Webb
 */
class FilteredHostExceptionTests {

	@Test
	void create() {
		InetAddressFilter matcher = (address) -> false;
		FilteredHostException exception = new FilteredHostException("localhost", matcher);
		assertThat(exception).hasMessage("Filtered host 'localhost'");
		assertThat(exception.getHost()).isEqualTo("localhost");
		assertThat(exception.getFilter()).isSameAs(matcher);
	}

}
