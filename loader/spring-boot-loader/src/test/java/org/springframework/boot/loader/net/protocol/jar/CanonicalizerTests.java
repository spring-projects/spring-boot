/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.loader.net.protocol.jar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Canonicalizer}.
 *
 * @author Phillip Webb
 */
class CanonicalizerTests {

	@Test
	void canonicalizeAfterOnlyChangesAfterPos() {
		String prefix = "/foo/.././bar/.!/foo/.././bar/.";
		String canonicalized = Canonicalizer.canonicalizeAfter(prefix, prefix.indexOf("!/"));
		assertThat(canonicalized).isEqualTo("/foo/.././bar/.!/bar/");
	}

	@Test
	void canonicalizeWhenHasEmbeddedSlashDotDotSlash() {
		assertThat(Canonicalizer.canonicalize("/foo/../bar/bif/bam/../../baz")).isEqualTo("/bar/baz");
	}

	@Test
	void canonicalizeWhenHasEmbeddedSlashDotSlash() {
		assertThat(Canonicalizer.canonicalize("/foo/./bar/bif/bam/././baz")).isEqualTo("/foo/bar/bif/bam/baz");
	}

	@Test
	void canonicalizeWhenHasTrailingSlashDotDot() {
		assertThat(Canonicalizer.canonicalize("/foo/bar/baz/../..")).isEqualTo("/foo/");
	}

	@Test
	void canonicalizeWhenHasTrailingSlashDot() {
		assertThat(Canonicalizer.canonicalize("/foo/bar/baz/./.")).isEqualTo("/foo/bar/baz/");
	}

}
