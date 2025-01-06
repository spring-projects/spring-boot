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
