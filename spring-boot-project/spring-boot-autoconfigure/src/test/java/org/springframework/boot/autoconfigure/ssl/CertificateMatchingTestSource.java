/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Source used with {@link CertificateMatchingTest @CertificateMatchingTest} annotated
 * tests that provides access to useful test material.
 *
 * @param algorithm the algorithm
 * @param privateKey the private key to use for matching
 * @param matchingCertificate a certificate that matches the private key
 * @param nonMatchingCertificates a list of certificate that do not match the private key
 * @param nonMatchingPrivateKeys a list of private keys that do not match the certificate
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
record CertificateMatchingTestSource(CertificateMatchingTestSource.Algorithm algorithm, PrivateKey privateKey,
		X509Certificate matchingCertificate, List<X509Certificate> nonMatchingCertificates,
		List<PrivateKey> nonMatchingPrivateKeys) {

	private static final List<Algorithm> ALGORITHMS;
	static {
		List<Algorithm> algorithms = new ArrayList<>();
		Stream.of("RSA", "DSA", "ed25519", "ed448").map(Algorithm::of).forEach(algorithms::add);
		Stream.of("secp256r1", "secp521r1").map(Algorithm::ec).forEach(algorithms::add);
		ALGORITHMS = List.copyOf(algorithms);
	}

	CertificateMatchingTestSource(Algorithm algorithm, KeyPair matchingKeyPair, List<KeyPair> nonMatchingKeyPairs) {
		this(algorithm, matchingKeyPair.getPrivate(), asCertificate(matchingKeyPair),
				nonMatchingKeyPairs.stream().map(CertificateMatchingTestSource::asCertificate).toList(),
				nonMatchingKeyPairs.stream().map(KeyPair::getPrivate).toList());
	}

	private static X509Certificate asCertificate(KeyPair keyPair) {
		X509Certificate certificate = mock(X509Certificate.class);
		given(certificate.getPublicKey()).willReturn(keyPair.getPublic());
		return certificate;
	}

	@Override
	public String toString() {
		return this.algorithm.toString();
	}

	static List<CertificateMatchingTestSource> create()
			throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		Map<Algorithm, KeyPair> keyPairs = new LinkedHashMap<>();
		for (Algorithm algorithm : ALGORITHMS) {
			keyPairs.put(algorithm, algorithm.generateKeyPair());
		}
		List<CertificateMatchingTestSource> parameters = new ArrayList<>();
		keyPairs.forEach((algorithm, matchingKeyPair) -> {
			List<KeyPair> nonMatchingKeyPairs = new ArrayList<>(keyPairs.values());
			nonMatchingKeyPairs.remove(matchingKeyPair);
			parameters.add(new CertificateMatchingTestSource(algorithm, matchingKeyPair, nonMatchingKeyPairs));
		});
		return List.copyOf(parameters);
	}

	/**
	 * An individual algorithm.
	 *
	 * @param name the algorithm name
	 * @param spec the algorithm spec or {@code null}
	 */
	record Algorithm(String name, AlgorithmParameterSpec spec) {

		KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(this.name);
			if (this.spec != null) {
				generator.initialize(this.spec);
			}
			return generator.generateKeyPair();
		}

		@Override
		public String toString() {
			String spec = (this.spec instanceof NamedParameterSpec namedSpec) ? namedSpec.getName() : "";
			return this.name + ((!spec.isEmpty()) ? ":" + spec : "");
		}

		static Algorithm of(String name) {
			return new Algorithm(name, null);
		}

		static Algorithm ec(String curve) {
			return new Algorithm("EC", new ECGenParameterSpec(curve));
		}

	}

}
