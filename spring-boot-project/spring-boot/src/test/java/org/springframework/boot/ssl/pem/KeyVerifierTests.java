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

package org.springframework.boot.ssl.pem;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.boot.ssl.pem.KeyVerifier.Result;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KeyVerifier}.
 *
 * @author Moritz Halbritter
 */
class KeyVerifierTests {

	private static final List<Algorithm> ALGORITHMS = List.of(Algorithm.of("RSA"), Algorithm.of("DSA"),
			Algorithm.of("ed25519"), Algorithm.of("ed448"), Algorithm.ec("secp256r1"), Algorithm.ec("secp521r1"));

	private final KeyVerifier keyVerifier = new KeyVerifier();

	@ParameterizedTest(name = "{0}")
	@MethodSource("arguments")
	void test(PrivateKey privateKey, PublicKey publicKey, List<PublicKey> invalidPublicKeys) {
		assertThat(this.keyVerifier.matches(privateKey, publicKey)).isEqualTo(Result.YES);
		for (PublicKey invalidPublicKey : invalidPublicKeys) {
			assertThat(this.keyVerifier.matches(privateKey, invalidPublicKey)).isEqualTo(Result.NO);
		}
	}

	static Stream<Arguments> arguments() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		List<KeyPair> keyPairs = new LinkedList<>();
		for (Algorithm algorithm : ALGORITHMS) {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm.name());
			if (algorithm.spec() != null) {
				generator.initialize(algorithm.spec());
			}
			keyPairs.add(generator.generateKeyPair());
			keyPairs.add(generator.generateKeyPair());
		}
		return keyPairs.stream()
			.map((kp) -> Arguments.arguments(Named.named(kp.getPrivate().getAlgorithm(), kp.getPrivate()),
					kp.getPublic(), without(keyPairs, kp).map(KeyPair::getPublic).toList()));
	}

	private static Stream<KeyPair> without(List<KeyPair> keyPairs, KeyPair without) {
		return keyPairs.stream().filter((kp) -> !kp.equals(without));
	}

	private record Algorithm(String name, AlgorithmParameterSpec spec) {
		static Algorithm of(String name) {
			return new Algorithm(name, null);
		}

		static Algorithm ec(String curve) {
			return new Algorithm("EC", new ECGenParameterSpec(curve));
		}
	}

}
