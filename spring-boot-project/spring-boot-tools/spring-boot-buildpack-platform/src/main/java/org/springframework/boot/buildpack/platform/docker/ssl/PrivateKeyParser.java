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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.buildpack.platform.docker.ssl.PrivateKeyParser.DerElement.TagType;
import org.springframework.boot.buildpack.platform.docker.ssl.PrivateKeyParser.DerElement.ValueType;
import org.springframework.util.Assert;

/**
 * Parser for PKCS private key files in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
final class PrivateKeyParser {

	private static final String PKCS1_RSA_HEADER = "-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String PKCS1_RSA_FOOTER = "-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String PKCS8_HEADER = "-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String PKCS8_FOOTER = "-+END\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String SEC1_EC_HEADER = "-+BEGIN\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String SEC1_EC_FOOTER = "-+END\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

	private static final List<PemParser> PEM_PARSERS;
	static {
		List<PemParser> parsers = new ArrayList<>();
		parsers
			.add(new PemParser(PKCS1_RSA_HEADER, PKCS1_RSA_FOOTER, PrivateKeyParser::createKeySpecForPkcs1Rsa, "RSA"));
		parsers.add(new PemParser(SEC1_EC_HEADER, SEC1_EC_FOOTER, PrivateKeyParser::createKeySpecForSec1Ec, "EC"));
		parsers.add(new PemParser(PKCS8_HEADER, PKCS8_FOOTER, PKCS8EncodedKeySpec::new, "RSA", "RSASSA-PSS", "EC",
				"DSA", "EdDSA", "XDH"));
		PEM_PARSERS = Collections.unmodifiableList(parsers);
	}

	/**
	 * ASN.1 encoded object identifier {@literal 1.2.840.113549.1.1.1}.
	 */
	private static final int[] RSA_ALGORITHM = { 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01 };

	/**
	 * ASN.1 encoded object identifier {@literal 1.2.840.10045.2.1}.
	 */
	private static final int[] EC_ALGORITHM = { 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01 };

	/**
	 * ASN.1 encoded object identifier {@literal 1.3.132.0.34}.
	 */
	private static final int[] EC_PARAMETERS = { 0x2b, 0x81, 0x04, 0x00, 0x22 };

	private PrivateKeyParser() {
	}

	private static PKCS8EncodedKeySpec createKeySpecForPkcs1Rsa(byte[] bytes) {
		return createKeySpecForAlgorithm(bytes, RSA_ALGORITHM, null);
	}

	private static PKCS8EncodedKeySpec createKeySpecForSec1Ec(byte[] bytes) {
		DerElement ecPrivateKey = DerElement.of(bytes);
		Assert.state(ecPrivateKey.isType(ValueType.ENCODED, TagType.SEQUENCE),
				"Key spec should be an ASN.1 encoded sequence");
		DerElement version = DerElement.of(ecPrivateKey.getContents());
		Assert.state(version != null && version.isType(ValueType.PRIMITIVE, TagType.INTEGER),
				"Key spec should start with version");
		Assert.state(version.getContents().remaining() == 1 && version.getContents().get() == 1,
				"Key spec version must be 1");
		DerElement privateKey = DerElement.of(ecPrivateKey.getContents());
		Assert.state(privateKey != null && privateKey.isType(ValueType.PRIMITIVE, TagType.OCTET_STRING),
				"Key spec should contain private key");
		DerElement parameters = DerElement.of(ecPrivateKey.getContents());
		return createKeySpecForAlgorithm(bytes, EC_ALGORITHM, getEcParameters(parameters));
	}

	private static int[] getEcParameters(DerElement parameters) {
		if (parameters == null) {
			return EC_PARAMETERS;
		}
		Assert.state(parameters.isType(ValueType.ENCODED), "Key spec should contain encoded parameters");
		DerElement contents = DerElement.of(parameters.getContents());
		Assert.state(contents.isType(ValueType.PRIMITIVE, TagType.OBJECT_IDENTIFIER),
				"Key spec parameters should contain object identifier");
		return getEcParameters(contents.getContents());
	}

	private static int[] getEcParameters(ByteBuffer bytes) {
		int[] result = new int[bytes.remaining()];
		for (int i = 0; i < result.length; i++) {
			result[i] = bytes.get() & 0xFF;
		}
		return result;
	}

	private static PKCS8EncodedKeySpec createKeySpecForAlgorithm(byte[] bytes, int[] algorithm, int[] parameters) {
		try {
			DerEncoder encoder = new DerEncoder();
			encoder.integer(0x00); // Version 0
			DerEncoder algorithmIdentifier = new DerEncoder();
			algorithmIdentifier.objectIdentifier(algorithm);
			algorithmIdentifier.objectIdentifier(parameters);
			byte[] byteArray = algorithmIdentifier.toByteArray();
			encoder.sequence(byteArray);
			encoder.octetString(bytes);
			return new PKCS8EncodedKeySpec(encoder.toSequence());
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Load a private key from the specified file paths.
	 * @param path the path to the private key file
	 * @return private key from specified file path
	 */
	static PrivateKey parse(Path path) {
		try {
			String text = Files.readString(path);
			for (PemParser pemParser : PEM_PARSERS) {
				PrivateKey privateKey = pemParser.parse(text);
				if (privateKey != null) {
					return privateKey;
				}
			}
			throw new IllegalStateException("Unrecognized private key format");
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error loading private key file " + path, ex);
		}
	}

	/**
	 * Parser for a specific PEM format.
	 */
	private static class PemParser {

		private final Pattern pattern;

		private final Function<byte[], PKCS8EncodedKeySpec> keySpecFactory;

		private final String[] algorithms;

		PemParser(String header, String footer, Function<byte[], PKCS8EncodedKeySpec> keySpecFactory,
				String... algorithms) {
			this.pattern = Pattern.compile(header + BASE64_TEXT + footer, Pattern.CASE_INSENSITIVE);
			this.algorithms = algorithms;
			this.keySpecFactory = keySpecFactory;
		}

		PrivateKey parse(String text) {
			Matcher matcher = this.pattern.matcher(text);
			return (!matcher.find()) ? null : parse(decodeBase64(matcher.group(1)));
		}

		private static byte[] decodeBase64(String content) {
			byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
			return Base64.getDecoder().decode(contentBytes);
		}

		private PrivateKey parse(byte[] bytes) {
			PKCS8EncodedKeySpec keySpec = this.keySpecFactory.apply(bytes);
			for (String algorithm : this.algorithms) {
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
					return keyFactory.generatePrivate(keySpec);
				}
				catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
				}
			}
			return null;
		}

	}

	/**
	 * Simple ASN.1 DER encoder.
	 */
	static class DerEncoder {

		private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

		void objectIdentifier(int... encodedObjectIdentifier) throws IOException {
			int code = (encodedObjectIdentifier != null) ? 0x06 : 0x05;
			codeLengthBytes(code, bytes(encodedObjectIdentifier));
		}

		void integer(int... encodedInteger) throws IOException {
			codeLengthBytes(0x02, bytes(encodedInteger));
		}

		void octetString(byte[] bytes) throws IOException {
			codeLengthBytes(0x04, bytes);
		}

		void sequence(int... elements) throws IOException {
			sequence(bytes(elements));
		}

		void sequence(byte[] bytes) throws IOException {
			codeLengthBytes(0x30, bytes);
		}

		void codeLengthBytes(int code, byte[] bytes) throws IOException {
			this.stream.write(code);
			int length = (bytes != null) ? bytes.length : 0;
			if (length <= 127) {
				this.stream.write(length & 0xFF);
			}
			else {
				ByteArrayOutputStream lengthStream = new ByteArrayOutputStream();
				while (length != 0) {
					lengthStream.write(length & 0xFF);
					length = length >> 8;
				}
				byte[] lengthBytes = lengthStream.toByteArray();
				this.stream.write(0x80 | lengthBytes.length);
				for (int i = lengthBytes.length - 1; i >= 0; i--) {
					this.stream.write(lengthBytes[i]);
				}
			}
			if (bytes != null) {
				this.stream.write(bytes);
			}
		}

		private static byte[] bytes(int... elements) {
			if (elements == null) {
				return null;
			}
			byte[] result = new byte[elements.length];
			for (int i = 0; i < elements.length; i++) {
				result[i] = (byte) elements[i];
			}
			return result;
		}

		byte[] toSequence() throws IOException {
			DerEncoder sequenceEncoder = new DerEncoder();
			sequenceEncoder.sequence(toByteArray());
			return sequenceEncoder.toByteArray();
		}

		byte[] toByteArray() {
			return this.stream.toByteArray();
		}

	}

	/**
	 * An ASN.1 DER encoded element.
	 */
	static final class DerElement {

		private final ValueType valueType;

		private final long tagType;

		private final ByteBuffer contents;

		private DerElement(ByteBuffer bytes) {
			byte b = bytes.get();
			this.valueType = ((b & 0x20) == 0) ? ValueType.PRIMITIVE : ValueType.ENCODED;
			this.tagType = decodeTagType(b, bytes);
			int length = decodeLength(bytes);
			bytes.limit(bytes.position() + length);
			this.contents = bytes.slice();
			bytes.limit(bytes.capacity());
			bytes.position(bytes.position() + length);
		}

		private long decodeTagType(byte b, ByteBuffer bytes) {
			long tagType = (b & 0x1F);
			if (tagType != 0x1F) {
				return tagType;
			}
			tagType = 0;
			b = bytes.get();
			while ((b & 0x80) != 0) {
				tagType <<= 7;
				tagType = tagType | (b & 0x7F);
				b = bytes.get();
			}
			return tagType;
		}

		private int decodeLength(ByteBuffer bytes) {
			byte b = bytes.get();
			if ((b & 0x80) == 0) {
				return b & 0x7F;
			}
			int numberOfLengthBytes = (b & 0x7F);
			Assert.state(numberOfLengthBytes != 0, "Infinite length encoding is not supported");
			Assert.state(numberOfLengthBytes != 0x7F, "Reserved length encoding is not supported");
			Assert.state(numberOfLengthBytes <= 4, "Length overflow");
			int length = 0;
			for (int i = 0; i < numberOfLengthBytes; i++) {
				length <<= 8;
				length |= (bytes.get() & 0xFF);
			}
			return length;
		}

		boolean isType(ValueType valueType) {
			return this.valueType == valueType;
		}

		boolean isType(ValueType valueType, TagType tagType) {
			return this.valueType == valueType && this.tagType == tagType.getNumber();
		}

		ByteBuffer getContents() {
			return this.contents;
		}

		static DerElement of(byte[] bytes) {
			return of(ByteBuffer.wrap(bytes));
		}

		static DerElement of(ByteBuffer bytes) {
			return (bytes.remaining() > 0) ? new DerElement(bytes) : null;
		}

		enum ValueType {

			PRIMITIVE, ENCODED

		}

		enum TagType {

			INTEGER(0x02), OCTET_STRING(0x04), OBJECT_IDENTIFIER(0x06), SEQUENCE(0x10);

			private final int number;

			TagType(int number) {
				this.number = number;
			}

			int getNumber() {
				return this.number;
			}

		}

	}

}
