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

package org.springframework.boot.buildpack.platform.docker.type;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test for {@link LayerId}.
 *
 * @author Phillip Webb
 */
class LayerIdTests {

	@Test
	void ofReturnsLayerId() {
		LayerId id = LayerId.of("sha256:9a183e56c86d376b408bdf922746d0a657f62b0e18c7c8f82a496b87710c576f");
		assertThat(id.getAlgorithm()).isEqualTo("sha256");
		assertThat(id.getHash()).isEqualTo("9a183e56c86d376b408bdf922746d0a657f62b0e18c7c8f82a496b87710c576f");
		assertThat(id).hasToString("sha256:9a183e56c86d376b408bdf922746d0a657f62b0e18c7c8f82a496b87710c576f");
	}

	@Test
	void hashCodeAndEquals() {
		LayerId id1 = LayerId.of("sha256:9a183e56c86d376b408bdf922746d0a657f62b0e18c7c8f82a496b87710c576f");
		LayerId id2 = LayerId.of("sha256:9a183e56c86d376b408bdf922746d0a657f62b0e18c7c8f82a496b87710c576f");
		LayerId id3 = LayerId.of("sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
		assertThat(id1).hasSameHashCodeAs(id2);
		assertThat(id1).isEqualTo(id1).isEqualTo(id2).isNotEqualTo(id3);
	}

	@Test
	void ofWhenValueIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.of((String) null))
			.withMessage("Value must not be empty");
	}

	@Test
	void ofWhenValueIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.of(" ")).withMessage("Value must not be empty");
	}

	@Test
	void ofSha256Digest() throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update("test".getBytes(StandardCharsets.UTF_8));
		LayerId id = LayerId.ofSha256Digest(digest.digest());
		assertThat(id).hasToString("sha256:9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
	}

	@Test
	void ofSha256DigestWithZeroPadding() {
		byte[] digest = new byte[32];
		Arrays.fill(digest, (byte) 127);
		digest[0] = 1;
		LayerId id = LayerId.ofSha256Digest(digest);
		assertThat(id).hasToString("sha256:017f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f");
	}

	@Test
	void ofSha256DigestWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.ofSha256Digest((byte[]) null))
			.withMessage("Digest must not be null");
	}

	@Test
	void ofSha256DigestWhenWrongLengthThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.ofSha256Digest(new byte[31]))
			.withMessage("Digest must be exactly 32 bytes");
	}

}
