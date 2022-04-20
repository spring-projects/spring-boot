/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.server;

import java.security.PrivateKey;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PrivateKeyParser}.
 *
 * @author Scott Frederick
 */
class PrivateKeyParserTests {

	@Test
	void parsePkcs8KeyFile() {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:test-key.pem");
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
	}

	@Test
	void parseWithNonKeyFileWillThrowException() {
		String path = "classpath:test-banner.txt";
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse("file://" + path))
				.withMessageContaining(path);
	}

	@Test
	void parseWithInvalidPathWillThrowException() {
		String path = "file:///bad/path/key.pem";
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse(path)).withMessageContaining(path);
	}

}
