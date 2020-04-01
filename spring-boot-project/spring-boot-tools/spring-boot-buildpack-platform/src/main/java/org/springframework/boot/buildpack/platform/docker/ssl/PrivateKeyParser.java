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

import java.io.ByteArrayOutputStream;
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
 * @author Phillip Webb
 */
final class PrivateKeyParser {

	private static final String PKCS1_HEADER = "-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String PKCS1_FOOTER = "-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String PKCS8_FOOTER = "-+END\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String PKCS8_HEADER = "-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

	private static final Pattern PKCS1_PATTERN = Pattern.compile(PKCS1_HEADER + BASE64_TEXT + PKCS1_FOOTER,
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PKCS8_KEY_PATTERN = Pattern.compile(PKCS8_HEADER + BASE64_TEXT + PKCS8_FOOTER,
			Pattern.CASE_INSENSITIVE);

	private PrivateKeyParser() {
	}

	/**
	 * Load a private key from the specified file paths.
	 * @param path the path to the private key file
	 * @return private key from specified file path
	 */
	static PrivateKey parse(Path path) {
		try {
			String text = readText(path);
			Matcher matcher = PKCS1_PATTERN.matcher(text);
			if (matcher.find()) {
				return parsePkcs1(decodeBase64(matcher.group(1)));
			}
			matcher = PKCS8_KEY_PATTERN.matcher(text);
			if (matcher.find()) {
				return parsePkcs8(decodeBase64(matcher.group(1)));
			}
			throw new IllegalStateException("Unrecognized private key format in " + path);
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error loading private key file " + path, ex);
		}
	}

	private static PrivateKey parsePkcs1(byte[] privateKeyBytes) throws GeneralSecurityException {
		byte[] pkcs8Bytes = convertPkcs1ToPkcs8(privateKeyBytes);
		return parsePkcs8(pkcs8Bytes);
	}

	private static byte[] convertPkcs1ToPkcs8(byte[] pkcs1) {
		try {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			int pkcs1Length = pkcs1.length;
			int totalLength = pkcs1Length + 22;
			// Sequence + total length
			result.write(bytes(0x30, 0x82));
			result.write((totalLength >> 8) & 0xff);
			result.write(totalLength & 0xff);
			// Integer (0)
			result.write(bytes(0x02, 0x01, 0x00));
			// Sequence: 1.2.840.113549.1.1.1, NULL
			result.write(
					bytes(0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00));
			// Octet string + length
			result.write(bytes(0x04, 0x82));
			result.write((pkcs1Length >> 8) & 0xff);
			result.write(pkcs1Length & 0xff);
			// PKCS1
			result.write(pkcs1);
			return result.toByteArray();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static byte[] bytes(int... elements) {
		byte[] result = new byte[elements.length];
		for (int i = 0; i < elements.length; i++) {
			result[i] = (byte) elements[i];
		}
		return result;
	}

	private static PrivateKey parsePkcs8(byte[] privateKeyBytes) throws GeneralSecurityException {
		try {
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePrivate(keySpec);
		}
		catch (InvalidKeySpecException ex) {
			throw new IllegalArgumentException("Unexpected key format", ex);
		}
	}

	private static String readText(Path path) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static byte[] decodeBase64(String content) {
		byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
		return Base64Utils.decode(contentBytes);
	}

}
