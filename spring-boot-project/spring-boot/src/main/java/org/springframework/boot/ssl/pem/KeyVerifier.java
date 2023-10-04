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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Performs checks on keys, e.g., if a public key and a private key belong together.
 *
 * @author Moritz Halbritter
 */
class KeyVerifier {

	private static final byte[] DATA = "Just some piece of data which gets signed".getBytes(StandardCharsets.UTF_8);

	/**
	 * Checks if the given private key belongs to the given public key.
	 * @param privateKey the private key
	 * @param publicKey the public key
	 * @return whether the keys belong together
	 */
	Result matches(PrivateKey privateKey, PublicKey publicKey) {
		try {
			if (!privateKey.getAlgorithm().equals(publicKey.getAlgorithm())) {
				// Keys are of different type
				return Result.NO;
			}
			String algorithm = getSignatureAlgorithm(privateKey.getAlgorithm());
			if (algorithm == null) {
				return Result.UNKNOWN;
			}
			byte[] signature = createSignature(privateKey, algorithm);
			return verifySignature(publicKey, algorithm, signature);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
			return Result.UNKNOWN;
		}
	}

	private static byte[] createSignature(PrivateKey privateKey, String algorithm)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signer = Signature.getInstance(algorithm);
		signer.initSign(privateKey);
		signer.update(DATA);
		return signer.sign();
	}

	private static Result verifySignature(PublicKey publicKey, String algorithm, byte[] signature)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature verifier = Signature.getInstance(algorithm);
		verifier.initVerify(publicKey);
		verifier.update(DATA);
		try {
			if (verifier.verify(signature)) {
				return Result.YES;
			}
			else {
				return Result.NO;
			}
		}
		catch (SignatureException ex) {
			return Result.NO;
		}
	}

	private static String getSignatureAlgorithm(String keyAlgorithm) {
		// https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms
		// https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keypairgenerator-algorithms
		return switch (keyAlgorithm) {
			case "RSA" -> "SHA256withRSA";
			case "DSA" -> "SHA256withDSA";
			case "EC" -> "SHA256withECDSA";
			case "EdDSA" -> "EdDSA";
			default -> null;
		};
	}

	enum Result {

		YES, NO, UNKNOWN

	}

}
