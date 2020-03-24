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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Base64Utils;

/**
 * Parser for PKCS private key files in PEM format.
 *
 * @author Scott Frederick
 */
final class PrivateKeyParser {

	private static final Pattern PKCS_1_KEY_PATTERN = Pattern
			.compile("-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
					"([a-z0-9+/=\\r\\n]+)" + // Base64 text
					"-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+", // Footer
					Pattern.CASE_INSENSITIVE);

	private static final Pattern PKCS_8_KEY_PATTERN = Pattern
			.compile("-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
					"([a-z0-9+/=\\r\\n]+)" + // Base64 text
					"-+END\\s+PRIVATE\\s+KEY[^-]*-+", // Footer
					Pattern.CASE_INSENSITIVE);

	private PrivateKeyParser() {
	}

	/**
	 * Load a private key from the specified file paths.
	 * @param keyPath the path to the private key file
	 * @return private key from specified file path
	 */
	static PrivateKey parse(Path keyPath) {
		try {
			byte[] keyBytes = Files.readAllBytes(keyPath);
			String keyString = new String(keyBytes, StandardCharsets.UTF_8);

			Matcher matcher = PKCS_1_KEY_PATTERN.matcher(keyString);
			if (matcher.find()) {
				return parsePkcs1PrivateKey(decodeContent(matcher.group(1)));
			}

			matcher = PKCS_8_KEY_PATTERN.matcher(keyString);
			if (matcher.find()) {
				return parsePkcs8PrivateKey(decodeContent(matcher.group(1)));
			}

			throw new IllegalStateException("Unrecognized private key format in " + keyPath);
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error loading private key file " + keyPath, ex);
		}
	}

	private static byte[] decodeContent(String content) {
		byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
		return Base64Utils.decode(contentBytes);
	}

	private static PrivateKey parsePkcs1PrivateKey(byte[] privateKeyBytes) throws GeneralSecurityException {
		byte[] pkcs8Bytes = convertPkcs1ToPkcs8(privateKeyBytes);
		return parsePkcs8PrivateKey(pkcs8Bytes);
	}

	private static PrivateKey parsePkcs8PrivateKey(byte[] privateKeyBytes) throws GeneralSecurityException {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(keySpec);
		}
		catch (InvalidKeySpecException ex) {
			throw new IllegalArgumentException("Unexpected key format", ex);
		}
	}

	private static byte[] convertPkcs1ToPkcs8(byte[] privateKeyBytes) {
		int pkcs1Length = privateKeyBytes.length;
		int totalLength = pkcs1Length + 22;
		byte[] pkcs8Header = new byte[] { 0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff),
				// Sequence + total length
				(byte) (totalLength & 0xff),
				// Integer (0)
				0x2, 0x1, 0x0,
				// Sequence: 1.2.840.113549.1.1.1, NULL
				0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0,
				// Octet string + length
				0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) };
		return join(pkcs8Header, privateKeyBytes);
	}

	private static byte[] join(byte[] byteArray1, byte[] byteArray2) {
		byte[] bytes = new byte[byteArray1.length + byteArray2.length];
		System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.length);
		System.arraycopy(byteArray2, 0, bytes, byteArray1.length, byteArray2.length);
		return bytes;
	}

}
