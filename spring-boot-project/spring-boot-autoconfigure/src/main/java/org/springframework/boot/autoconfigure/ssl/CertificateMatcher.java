/*
 * Copyright 2012-2025 the original author or authors.
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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Helper used to match certificates against a {@link PrivateKey}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class CertificateMatcher {

	private static final byte[] DATA = new byte[256];
	static {
		for (int i = 0; i < DATA.length; i++) {
			DATA[i] = (byte) i;
		}
	}

	private final PrivateKey privateKey;

	private final Signature signature;

	private final byte[] generatedSignature;

	CertificateMatcher(PrivateKey privateKey) {
		Assert.notNull(privateKey, "'privateKey' must not be null");
		this.privateKey = privateKey;
		this.signature = createSignature(privateKey);
		Assert.state(this.signature != null, "Failed to create signature");
		this.generatedSignature = sign(this.signature, privateKey);
	}

	private Signature createSignature(PrivateKey privateKey) {
		try {
			String algorithm = getSignatureAlgorithm(privateKey);
			return (algorithm != null) ? Signature.getInstance(algorithm) : null;
		}
		catch (NoSuchAlgorithmException ex) {
			return null;
		}
	}

	private static String getSignatureAlgorithm(PrivateKey privateKey) {
		// https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms
		// https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keypairgenerator-algorithms
		return switch (privateKey.getAlgorithm()) {
			case "RSA" -> "SHA256withRSA";
			case "DSA" -> "SHA256withDSA";
			case "EC" -> "SHA256withECDSA";
			case "EdDSA" -> "EdDSA";
			default -> null;
		};
	}

	boolean matchesAny(List<? extends Certificate> certificates) {
		return (this.generatedSignature != null) && certificates.stream().anyMatch(this::matches);
	}

	boolean matches(Certificate certificate) {
		return matches(certificate.getPublicKey());
	}

	private boolean matches(PublicKey publicKey) {
		return (this.generatedSignature != null)
				&& Objects.equals(this.privateKey.getAlgorithm(), publicKey.getAlgorithm()) && verify(publicKey);
	}

	private boolean verify(PublicKey publicKey) {
		try {
			this.signature.initVerify(publicKey);
			this.signature.update(DATA);
			return this.signature.verify(this.generatedSignature);
		}
		catch (InvalidKeyException | SignatureException ex) {
			return false;
		}
	}

	private static byte[] sign(Signature signature, PrivateKey privateKey) {
		try {
			signature.initSign(privateKey);
			signature.update(DATA);
			return signature.sign();
		}
		catch (InvalidKeyException | SignatureException ex) {
			return null;
		}
	}

}
