/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationprocessor.metadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ItemMetadata}.
 *
 * @author Stephane Nicoll
 */
class ItemMetadataTests {

	@Test
	void newItemMetadataPrefixWithCapitalizedPrefix() {
		assertThat(newItemMetadataPrefix("Prefix.", "value")).isEqualTo("prefix.value");
	}

	@Test
	void newItemMetadataPrefixWithCamelCaseSuffix() {
		assertThat(newItemMetadataPrefix("prefix.", "myValue")).isEqualTo("prefix.my-value");
	}

	@Test
	void newItemMetadataPrefixWithUpperCamelCaseSuffix() {
		assertThat(newItemMetadataPrefix("prefix.", "MyValue")).isEqualTo("prefix.my-value");
	}

	private String newItemMetadataPrefix(String prefix, String suffix) {
		return ItemMetadata.newItemMetadataPrefix(prefix, suffix);
	}

}
