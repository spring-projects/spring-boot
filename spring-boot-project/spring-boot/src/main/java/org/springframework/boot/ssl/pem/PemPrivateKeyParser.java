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

package org.springframework.boot.ssl.pem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.boot.ssl.pem.PemPrivateKeyParser.DerElement.TagType;
import org.springframework.boot.ssl.pem.PemPrivateKeyParser.DerElement.ValueType;
import org.springframework.util.Assert;

/**
 * Parser for PKCS private key files in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
final class PemPrivateKeyParser {

	private static final String PKCS1_RSA_HEADER = "-+BEGIN\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String PKCS1_RSA_FOOTER = "-+END\\s+RSA\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String PKCS8_HEADER = "-+BEGIN\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String PKCS8_FOOTER = "-+END\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String PKCS8_ENCRYPTED_HEADER = "-+BEGIN\\s+ENCRYPTED\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String PKCS8_ENCRYPTED_FOOTER = "-+END\\s+ENCRYPTED\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String SEC1_EC_HEADER = "-+BEGIN\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String SEC1_EC_FOOTER = "-+END\\s+EC\\s+PRIVATE\\s+KEY[^-]*-+";

	private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

	public static final int BASE64_TEXT_GROUP = 1;

	private static final EncodedOid RSA_ALGORITHM = EncodedOid.OID_1_2_840_113549_1_1_1;

	private static final EncodedOid ELLIPTIC_CURVE_ALGORITHM = EncodedOid.OID_1_2_840_10045_2_1;

	private static final EncodedOid ELLIPTIC_CURVE_384_BIT = EncodedOid.OID_1_3_132_0_34;

	private static final Map<EncodedOid, String> ALGORITHMS;
	static {
		Map<EncodedOid, String> algorithms = new HashMap<>();
		algorithms.put(EncodedOid.OID_1_2_840_113549_1_1_1, "RSA");
		algorithms.put(EncodedOid.OID_1_2_840_113549_1_1_10, "RSA");
		algorithms.put(EncodedOid.OID_1_2_840_10040_4_1, "DSA");
		algorithms.put(EncodedOid.OID_1_3_101_110, "XDH");
		algorithms.put(EncodedOid.OID_1_3_101_111, "XDH");
		algorithms.put(EncodedOid.OID_1_3_101_112, "EdDSA");
		algorithms.put(EncodedOid.OID_1_3_101_113, "EdDSA");
		algorithms.put(EncodedOid.OID_1_2_840_10045_2_1, "EC");
		ALGORITHMS = Collections.unmodifiableMap(algorithms);
	}

	private static final List<PemParser> PEM_PARSERS;
	static {
		List<PemParser> parsers = new ArrayList<>();
		parsers.add(new PemParser(PKCS1_RSA_HEADER, PKCS1_RSA_FOOTER, PemPrivateKeyParser::createKeySpecForPkcs1Rsa,
				"RSA"));
		parsers.add(new PemParser(SEC1_EC_HEADER, SEC1_EC_FOOTER, PemPrivateKeyParser::createKeySpecForSec1Ec, "EC"));
		parsers.add(new PemParser(PKCS8_HEADER, PKCS8_FOOTER, PemPrivateKeyParser::createKeySpecForPkcs8, "RSA",
				"RSASSA-PSS", "EC", "DSA", "EdDSA", "XDH"));
		parsers.add(new PemParser(PKCS8_ENCRYPTED_HEADER, PKCS8_ENCRYPTED_FOOTER,
				PemPrivateKeyParser::createKeySpecForPkcs8Encrypted, "RSA", "RSASSA-PSS", "EC", "DSA", "EdDSA", "XDH"));
		PEM_PARSERS = Collections.unmodifiableList(parsers);
	}

	private PemPrivateKeyParser() {
	}

	private static PKCS8EncodedKeySpec createKeySpecForPkcs1Rsa(byte[] bytes, String password) {
		return createKeySpecForAlgorithm(bytes, RSA_ALGORITHM, null);
	}

	private static PKCS8EncodedKeySpec createKeySpecForSec1Ec(byte[] bytes, String password) {
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
		return createKeySpecForAlgorithm(bytes, ELLIPTIC_CURVE_ALGORITHM, getEcParameters(parameters));
	}

	private static EncodedOid getEcParameters(DerElement parameters) {
		if (parameters == null) {
			return ELLIPTIC_CURVE_384_BIT;
		}
		Assert.state(parameters.isType(ValueType.ENCODED), "Key spec should contain encoded parameters");
		DerElement contents = DerElement.of(parameters.getContents());
		Assert.state(contents != null && contents.isType(ValueType.PRIMITIVE, TagType.OBJECT_IDENTIFIER),
				"Key spec parameters should contain object identifier");
		return EncodedOid.of(contents);
	}

	private static PKCS8EncodedKeySpec createKeySpecForAlgorithm(byte[] bytes, EncodedOid algorithm,
			EncodedOid parameters) {
		try {
			DerEncoder encoder = new DerEncoder();
			encoder.integer(0x00); // Version 0
			DerEncoder algorithmIdentifier = new DerEncoder();
			algorithmIdentifier.objectIdentifier(algorithm);
			algorithmIdentifier.objectIdentifier(parameters);
			encoder.sequence(algorithmIdentifier.toByteArray());
			encoder.octetString(bytes);
			return new PKCS8EncodedKeySpec(encoder.toSequence());
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static PKCS8EncodedKeySpec createKeySpecForPkcs8(byte[] bytes, String password) {
		DerElement ecPrivateKey = DerElement.of(bytes);
		Assert.state(ecPrivateKey.isType(ValueType.ENCODED, TagType.SEQUENCE),
				"Key spec should be an ASN.1 encoded sequence");
		DerElement version = DerElement.of(ecPrivateKey.getContents());
		Assert.state(version != null && version.isType(ValueType.PRIMITIVE, TagType.INTEGER),
				"Key spec should start with version");
		DerElement sequence = DerElement.of(ecPrivateKey.getContents());
		Assert.state(sequence != null && sequence.isType(ValueType.ENCODED, TagType.SEQUENCE),
				"Key spec should contain private key");
		DerElement algorithmId = DerElement.of(sequence.getContents());
		Assert.state(algorithmId != null && algorithmId.isType(ValueType.PRIMITIVE, TagType.OBJECT_IDENTIFIER),
				"Key spec container object identifier");
		String algorithmName = ALGORITHMS.get(EncodedOid.of(algorithmId));
		return (algorithmName != null) ? new PKCS8EncodedKeySpec(bytes, algorithmName) : new PKCS8EncodedKeySpec(bytes);
	}

	private static PKCS8EncodedKeySpec createKeySpecForPkcs8Encrypted(byte[] bytes, String password) {
		return Pkcs8PrivateKeyDecryptor.decrypt(bytes, password);
	}

	/**
	 * Parse a private key from the specified string.
	 * @param text the text to parse
	 * @return the parsed private key
	 */
	static PrivateKey parse(String text) {
		return parse(text, null);
	}

	/**
	 * Parse a private key from the specified string, using the provided password for
	 * decryption if necessary.
	 * @param text the text to parse
	 * @param password the password used to decrypt an encrypted private key
	 * @return the parsed private key
	 */
	static PrivateKey parse(String text, String password) {
		if (text == null) {
			return null;
		}
		try {
			for (PemParser pemParser : PEM_PARSERS) {
				PrivateKey privateKey = pemParser.parse(text, password);
				if (privateKey != null) {
					return privateKey;
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error loading private key file: " + ex.getMessage(), ex);
		}
		throw new IllegalStateException("Missing private key or unrecognized format");
	}

	/**
	 * Parser for a specific PEM format.
	 */
	private static class PemParser {

		private final Pattern pattern;

		private final BiFunction<byte[], String, PKCS8EncodedKeySpec> keySpecFactory;

		private final String[] algorithms;

		PemParser(String header, String footer, BiFunction<byte[], String, PKCS8EncodedKeySpec> keySpecFactory,
				String... algorithms) {
			this.pattern = Pattern.compile(header + BASE64_TEXT + footer, Pattern.CASE_INSENSITIVE);
			this.keySpecFactory = keySpecFactory;
			this.algorithms = algorithms;
		}

		PrivateKey parse(String text, String password) {
			Matcher matcher = this.pattern.matcher(text);
			return (!matcher.find()) ? null : parse(decodeBase64(matcher.group(BASE64_TEXT_GROUP)), password);
		}

		private static byte[] decodeBase64(String content) {
			byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
			return Base64.getDecoder().decode(contentBytes);
		}

		private PrivateKey parse(byte[] bytes, String password) {
			PKCS8EncodedKeySpec keySpec = this.keySpecFactory.apply(bytes, password);
			if (keySpec.getAlgorithm() != null) {
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(keySpec.getAlgorithm());
					return keyFactory.generatePrivate(keySpec);
				}
				catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
					// Ignore
				}
			}
			for (String algorithm : this.algorithms) {
				try {
					KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
					return keyFactory.generatePrivate(keySpec);
				}
				catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
					// Ignore
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

		void objectIdentifier(EncodedOid encodedOid) throws IOException {
			int code = (encodedOid != null) ? 0x06 : 0x05;
			codeLengthBytes(code, (encodedOid != null) ? encodedOid.toByteArray() : null);
		}

		void integer(int... encodedInteger) throws IOException {
			codeLengthBytes(0x02, bytes(encodedInteger));
		}

		void octetString(byte[] bytes) throws IOException {
			codeLengthBytes(0x04, bytes);
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

	/**
	 * Decryptor for PKCS8 encoded private keys.
	 */
	static class Pkcs8PrivateKeyDecryptor {

		public static final String PBES2_ALGORITHM = "PBES2";

		static PKCS8EncodedKeySpec decrypt(byte[] bytes, String password) {
			Assert.notNull(password, "Password is required for an encrypted private key");
			try {
				EncryptedPrivateKeyInfo keyInfo = new EncryptedPrivateKeyInfo(bytes);
				AlgorithmParameters algorithmParameters = keyInfo.getAlgParameters();
				String encryptionAlgorithm = getEncryptionAlgorithm(algorithmParameters, keyInfo.getAlgName());
				SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptionAlgorithm);
				SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray()));
				Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
				cipher.init(Cipher.DECRYPT_MODE, key, algorithmParameters);
				return keyInfo.getKeySpec(cipher);
			}
			catch (IOException | GeneralSecurityException ex) {
				throw new IllegalArgumentException("Error decrypting private key", ex);
			}
		}

		private static String getEncryptionAlgorithm(AlgorithmParameters algParameters, String algName) {
			if (algParameters != null && PBES2_ALGORITHM.equals(algName)) {
				return algParameters.toString();
			}
			return algName;
		}

	}

	/**
	 * ANS.1 encoded object identifier.
	 */
	static final class EncodedOid {

		static final EncodedOid OID_1_2_840_10040_4_1 = EncodedOid.of("2a8648ce380401");
		static final EncodedOid OID_1_2_840_113549_1_1_1 = EncodedOid.of("2A864886F70D010101");
		static final EncodedOid OID_1_2_840_113549_1_1_10 = EncodedOid.of("2a864886f70d01010a");
		static final EncodedOid OID_1_3_101_110 = EncodedOid.of("2b656e");
		static final EncodedOid OID_1_3_101_111 = EncodedOid.of("2b656f");
		static final EncodedOid OID_1_3_101_112 = EncodedOid.of("2b6570");
		static final EncodedOid OID_1_3_101_113 = EncodedOid.of("2b6571");
		static final EncodedOid OID_1_2_840_10045_2_1 = EncodedOid.of("2a8648ce3d0201");
		static final EncodedOid OID_1_3_132_0_34 = EncodedOid.of("2b81040022");

		private final byte[] value;

		private EncodedOid(byte[] value) {
			this.value = value;
		}

		byte[] toByteArray() {
			return this.value.clone();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return Arrays.equals(this.value, ((EncodedOid) obj).value);
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(this.value);
		}

		static EncodedOid of(String hexString) {
			return of(HexFormat.of().parseHex(hexString));
		}

		static EncodedOid of(DerElement derElement) {
			return of(derElement.getContents());
		}

		static EncodedOid of(ByteBuffer byteBuffer) {
			return of(byteBuffer.array(), byteBuffer.arrayOffset() + byteBuffer.position(), byteBuffer.remaining());
		}

		static EncodedOid of(byte[] bytes) {
			return of(bytes, 0, bytes.length);
		}

		static EncodedOid of(byte[] bytes, int off, int len) {
			byte[] value = new byte[len];
			System.arraycopy(bytes, off, value, 0, len);
			return new EncodedOid(value);
		}

	}

}
