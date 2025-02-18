/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
