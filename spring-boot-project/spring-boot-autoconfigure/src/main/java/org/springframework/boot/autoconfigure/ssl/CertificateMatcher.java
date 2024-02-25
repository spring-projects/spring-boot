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

	/**
	 * Constructs a new CertificateMatcher object with the provided private key.
	 * @param privateKey the private key to be used for signature generation
	 * @throws IllegalArgumentException if the private key is null
	 * @throws IllegalArgumentException if the signature creation fails
	 */
	CertificateMatcher(PrivateKey privateKey) {
		Assert.notNull(privateKey, "Private key must not be null");
		this.privateKey = privateKey;
		this.signature = createSignature(privateKey);
		Assert.notNull(this.signature, "Failed to create signature");
		this.generatedSignature = sign(this.signature, privateKey);
	}

	/**
	 * Creates a signature object using the provided private key.
	 * @param privateKey the private key to be used for creating the signature
	 * @return the signature object created using the private key, or null if the
	 * algorithm is not supported
	 */
	private Signature createSignature(PrivateKey privateKey) {
		try {
			String algorithm = getSignatureAlgorithm(privateKey);
			return (algorithm != null) ? Signature.getInstance(algorithm) : null;
		}
		catch (NoSuchAlgorithmException ex) {
			return null;
		}
	}

	/**
	 * Returns the signature algorithm corresponding to the given private key.
	 * @param privateKey the private key for which to determine the signature algorithm
	 * @return the signature algorithm corresponding to the private key, or null if the
	 * algorithm is not recognized
	 * @see <a href=
	 * "https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#signature-algorithms">Signature
	 * Algorithms</a>
	 * @see <a href=
	 * "https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#keypairgenerator-algorithms">Key
	 * Pair Generator Algorithms</a>
	 */
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

	/**
	 * Checks if the generated signature matches any of the certificates in the given
	 * list.
	 * @param certificates the list of certificates to check against
	 * @return true if the generated signature matches any of the certificates, false
	 * otherwise
	 */
	boolean matchesAny(List<? extends Certificate> certificates) {
		return (this.generatedSignature != null) && certificates.stream().anyMatch(this::matches);
	}

	/**
	 * Checks if the given certificate matches the public key of the certificate.
	 * @param certificate the certificate to be checked
	 * @return true if the certificate matches the public key, false otherwise
	 */
	boolean matches(Certificate certificate) {
		return matches(certificate.getPublicKey());
	}

	/**
	 * Checks if the given public key matches the generated signature.
	 * @param publicKey the public key to be checked
	 * @return true if the public key matches the generated signature, false otherwise
	 */
	private boolean matches(PublicKey publicKey) {
		return (this.generatedSignature != null)
				&& Objects.equals(this.privateKey.getAlgorithm(), publicKey.getAlgorithm()) && verify(publicKey);
	}

	/**
	 * Verifies the authenticity of a public key by comparing it with a generated
	 * signature.
	 * @param publicKey the public key to be verified
	 * @return true if the public key is authentic, false otherwise
	 */
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

	/**
	 * Signs the given data using the provided signature algorithm and private key.
	 * @param signature the signature algorithm to use for signing
	 * @param privateKey the private key to use for signing
	 * @return the signed data as a byte array, or null if an error occurred during
	 * signing
	 */
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
