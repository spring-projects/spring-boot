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

package org.springframework.boot.web.server;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PrivateKeyParser}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
// https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html#GUID-091BF58C-82AB-4C9C-850F-1660824D5254
class PrivateKeyParserTests {

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"dsa.key,		DSA",
			"rsa.key,		RSA",
			"rsa-pss.key,	RSASSA-PSS"
	})
	// @formatter:on
	void shouldParseTraditionalPkcs8(String file, String algorithm) {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs8/" + file);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"rsa.key,	RSA"
	})
	// @formatter:on
	void shouldParseTraditionalPkcs1(String file, String algorithm) {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs1/" + file);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"dsa.key"
	})
	// @formatter:on
	void shouldNotParseUnsupportedTraditionalPkcs1(String file) {
		assertThatThrownBy(() -> PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs1/" + file))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Error loading private key file")
			.hasCauseInstanceOf(IllegalStateException.class)
			.getCause()
			.hasMessageContaining("Unrecognized private key format");
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"brainpoolP256r1.key,	brainpoolP256r1,	1.3.36.3.3.2.8.1.1.7",
			"brainpoolP320r1.key,	brainpoolP320r1,	1.3.36.3.3.2.8.1.1.9",
			"brainpoolP384r1.key,	brainpoolP384r1,	1.3.36.3.3.2.8.1.1.11",
			"brainpoolP512r1.key,	brainpoolP512r1,	1.3.36.3.3.2.8.1.1.13",
			"prime256v1.key,		secp256r1,			1.2.840.10045.3.1.7",
			"secp224r1.key,			secp224r1,			1.3.132.0.33",
			"secp256k1.key,			secp256k1,			1.3.132.0.10",
			"secp256r1.key,			secp256r1,			1.2.840.10045.3.1.7",
			"secp384r1.key,			secp384r1,			1.3.132.0.34",
			"secp521r1.key,			secp521r1,			1.3.132.0.35"
	})
	// @formatter:on
	void shouldParseEcPkcs8(String file, String curveName, String oid) {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs8/" + file);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey).isInstanceOf(ECPrivateKey.class);
		ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
		assertThat(ecPrivateKey.getParams().toString()).contains(curveName).contains(oid);
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"brainpoolP256t1.key",
			"brainpoolP320t1.key",
			"brainpoolP384t1.key",
			"brainpoolP512t1.key"
	})
	// @formatter:on
	void shouldNotParseUnsupportedEcPkcs8(String file) {
		assertThatThrownBy(() -> PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs8/" + file))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Error loading private key file")
			.hasCauseInstanceOf(IllegalStateException.class)
			.getCause()
			.hasMessageContaining("Unrecognized private key format");
	}

	@EnabledForJreRange(min = JRE.JAVA_17, disabledReason = "EdDSA is only supported since Java 17")
	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"ed448.key",
			"ed25519.key"
	})
	// @formatter:on
	void shouldParseEdDsaPkcs8(String file) {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs8/" + file);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
	}

	@EnabledForJreRange(min = JRE.JAVA_17, disabledReason = "XDH is only supported since Java 17")
	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"x448.key",
			"x25519.key"
	})
		// @formatter:on
	void shouldParseXdhPkcs8(String file) {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/pkcs8/" + file);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("XDH");
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"brainpoolP256r1.key,	brainpoolP256r1,	1.3.36.3.3.2.8.1.1.7",
			"brainpoolP320r1.key,	brainpoolP320r1,	1.3.36.3.3.2.8.1.1.9",
			"brainpoolP384r1.key,	brainpoolP384r1,	1.3.36.3.3.2.8.1.1.11",
			"brainpoolP512r1.key,	brainpoolP512r1,	1.3.36.3.3.2.8.1.1.13",
			"prime256v1.key,		secp256r1,			1.2.840.10045.3.1.7",
			"secp224r1.key,			secp224r1,			1.3.132.0.33",
			"secp256k1.key,			secp256k1,			1.3.132.0.10",
			"secp256r1.key,			secp256r1,			1.2.840.10045.3.1.7",
			"secp384r1.key,			secp384r1,			1.3.132.0.34",
			"secp521r1.key,			secp521r1,			1.3.132.0.35"
	})
	// @formatter:on
	void shouldParseEcSec1(String file, String curveName, String oid) {
		PrivateKey privateKey = PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/sec1/" + file);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey).isInstanceOf(ECPrivateKey.class);
		ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
		assertThat(ecPrivateKey.getParams().toString()).contains(curveName).contains(oid);
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"brainpoolP256t1.key",
			"brainpoolP320t1.key",
			"brainpoolP384t1.key",
			"brainpoolP512t1.key"
	})
	// @formatter:on
	void shouldNotParseUnsupportedEcSec1(String file) {
		assertThatThrownBy(() -> PrivateKeyParser.parse("classpath:org/springframework/boot/web/server/sec1/" + file))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Error loading private key file")
			.hasCauseInstanceOf(IllegalStateException.class)
			.getCause()
			.hasMessageContaining("Unrecognized private key format");
	}

	@Test
	void parseWithNonKeyFileWillThrowException() {
		String path = "classpath:test-banner.txt";
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse("file://" + path))
			.withMessageContaining(path);
	}

	@Test
	void parseWithInvalidPathWillThrowException() {
		String path = "file:///bad/path/key.pem";
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse(path)).withMessageContaining(path);
	}

}
