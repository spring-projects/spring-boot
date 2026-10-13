/*
 * Copyright 2012-present the original author or authors.
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
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenValueIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.of((String) null))
			.withMessage("'value' must not be empty");
	}

	@Test
	void ofWhenValueIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.of(" ")).withMessage("'value' must not be empty");
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
	@SuppressWarnings("NullAway") // Test null check
	void ofSha256DigestWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.ofSha256Digest((byte[]) null))
			.withMessage("'digest' must not be null");
	}

	@Test
	void ofSha256DigestWhenWrongLengthThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> LayerId.ofSha256Digest(new byte[31]))
			.withMessage("'digest' must be exactly 32 bytes");
	}

}
