/*
 * Copyright 2012-2020 the original author or authors.
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

import java.math.BigInteger;

import org.springframework.util.Assert;

/**
 * A layer ID as used inside a Docker image of the form {@code algorithm: hash}.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class LayerId {

	private final String value;

	private final String algorithm;

	private final String hash;

	private LayerId(String value, String algorithm, String hash) {
		this.value = value;
		this.algorithm = algorithm;
		this.hash = hash;
	}

	/**
	 * Return the algorithm of layer.
	 * @return the algorithm
	 */
	public String getAlgorithm() {
		return this.algorithm;
	}

	/**
	 * Return the hash of the layer.
	 * @return the layer hash
	 */
	public String getHash() {
		return this.hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return this.value.equals(((LayerId) obj).value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return this.value;
	}

	/**
	 * Create a new {@link LayerId} with the specified value.
	 * @param value the layer ID value of the form {@code algorithm: hash}
	 * @return a new layer ID instance
	 */
	public static LayerId of(String value) {
		Assert.hasText(value, "Value must not be empty");
		int i = value.indexOf(':');
		Assert.isTrue(i >= 0, () -> "Invalid layer ID '" + value + "'");
		return new LayerId(value, value.substring(0, i), value.substring(i + 1));
	}

	/**
	 * Create a new {@link LayerId} from a SHA-256 digest.
	 * @param digest the digest
	 * @return a new layer ID instance
	 */
	public static LayerId ofSha256Digest(byte[] digest) {
		Assert.notNull(digest, "Digest must not be null");
		Assert.isTrue(digest.length == 32, "Digest must be exactly 32 bytes");
		String algorithm = "sha256";
		String hash = String.format("%32x", new BigInteger(1, digest));
		return new LayerId(algorithm + ":" + hash, algorithm, hash);
	}

}
