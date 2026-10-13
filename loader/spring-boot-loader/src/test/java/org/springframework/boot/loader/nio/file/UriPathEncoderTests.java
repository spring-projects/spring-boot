/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.nio.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UriPathEncoder}.
 *
 * @author Phillip Webb
 */
class UriPathEncoderTests {

	@Test
	void encodePath() {
		assertThat(UriPathEncoder.encode("/foo/bar")).isEqualTo("/foo/bar");
		assertThat(UriPathEncoder.encode("/foo bar")).isEqualTo("/foo%20bar");
		assertThat(UriPathEncoder.encode("/Z\u00fcrich")).isEqualTo("/Z%C3%BCrich");
	}

	@Test
	void encodePathWhenNoEncodingIsRequiredReturnsSameInstance() {
		String path = "/foo/bar";
		assertThat(UriPathEncoder.encode(path)).isSameAs(path);
	}

}
