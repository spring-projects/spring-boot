/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link UrlHostFilter}.
 *
 * @author Phillip Webb
 */
class UrlHostFilterTests {

	@Test
	void throwIfFilteredWhenFilteredThrowsException() {
		UrlHostFilter filter = new UrlHostFilter(InetAddressFilter.externalAddresses());
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> filter.throwIfFiltered(new URI("http://localhost")));
	}

	@Test
	void throwIfFilteredWhenNotFilteredDoesNotThrowsException() {
		UrlHostFilter filter = new UrlHostFilter(InetAddressFilter.internalAddresses());
		assertThatNoException().isThrownBy(() -> filter.throwIfFiltered(new URI("http://localhost")));
	}

}
