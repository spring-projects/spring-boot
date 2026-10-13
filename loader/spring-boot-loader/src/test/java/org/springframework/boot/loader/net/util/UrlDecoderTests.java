/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UrlDecoder}.
 *
 * @author Phillip Webb
 */
class UrlDecoderTests {

	@Test
	void decodeWhenBasicString() {
		assertThat(UrlDecoder.decode("a/b/C.class")).isEqualTo("a/b/C.class");
	}

	@Test
	void decodeWhenHasSingleByteEncodedCharacters() {
		assertThat(UrlDecoder.decode("%61/%62/%43.class")).isEqualTo("a/b/C.class");
	}

	@Test
	void decodeWhenHasDoubleByteEncodedCharacters() {
		assertThat(UrlDecoder.decode("%c3%a1/b/C.class")).isEqualTo("\u00e1/b/C.class");
	}

	@Test
	void decodeWhenHasMixtureOfEncodedAndUnencodedDoubleByteCharacters() {
		assertThat(UrlDecoder.decode("%c3%a1/b/\u00c7.class")).isEqualTo("\u00e1/b/\u00c7.class");
	}

}
