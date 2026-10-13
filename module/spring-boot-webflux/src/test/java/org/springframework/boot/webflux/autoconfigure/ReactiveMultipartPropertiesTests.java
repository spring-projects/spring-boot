/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveMultipartProperties}
 *
 * @author Chris Bono
 */
class ReactiveMultipartPropertiesTests {

	@Test
	void defaultValuesAreConsistent() {
		ReactiveMultipartProperties multipartProperties = new ReactiveMultipartProperties();
		DefaultPartHttpMessageReader defaultPartHttpMessageReader = new DefaultPartHttpMessageReader();
		assertThat(defaultPartHttpMessageReader).hasFieldOrPropertyWithValue("maxInMemorySize",
				(int) multipartProperties.getMaxInMemorySize().toBytes());
		assertThat(defaultPartHttpMessageReader).hasFieldOrPropertyWithValue("maxHeadersSize",
				(int) multipartProperties.getMaxHeadersSize().toBytes());
		assertThat(defaultPartHttpMessageReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart",
				multipartProperties.getMaxDiskUsagePerPart().toBytes());
		assertThat(defaultPartHttpMessageReader).hasFieldOrPropertyWithValue("maxParts",
				multipartProperties.getMaxParts());
	}

}
