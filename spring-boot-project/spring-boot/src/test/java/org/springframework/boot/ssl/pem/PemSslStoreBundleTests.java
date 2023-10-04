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

import java.security.KeyStore;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PemSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class PemSslStoreBundleTests {

	private static final char[] EMPTY_KEY_PASSWORD = new char[] {};

	@Test
	void whenNullStores() {
		PemSslStoreDetails keyStoreDetails = null;
		PemSslStoreDetails trustStoreDetails = null;
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void whenStoresHaveNoValues() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate(null);
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate(null);
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void whenHasKeyStoreDetailsCertAndKey() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = null;
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void whenHasKeyStoreDetailsCertAndEncryptedKey() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:ssl/pkcs8/key-rsa-encrypted.pem")
			.withPrivateKeyPassword("test");
		PemSslStoreDetails trustStoreDetails = null;
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void whenHasKeyStoreDetailsAndTrustStoreDetailsWithoutKey() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCert("ssl-0"));
	}

	@Test
	void whenHasKeyStoreDetailsAndTrustStoreDetails() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("ssl"));
	}

	@Test
	void whenHasKeyStoreDetailsAndTrustStoreDetailsAndAlias() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, "test-alias");
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("test-alias"));
	}

	@Test
	void whenHasStoreType() {
		PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails("PKCS12", "classpath:test-cert.pem",
				"classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails("PKCS12", "classpath:test-cert.pem",
				"classpath:test-key.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("PKCS12", "ssl"));
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("PKCS12", "ssl"));
	}

	@Test
	void whenHasKeyStoreDetailsAndTrustStoreDetailsAndKeyPassword() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails, "test-alias", "keysecret");
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
		assertThat(bundle.getTrustStore())
			.satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
	}

	@Test
	void shouldVerifyKeysIfEnabled() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails
			.forCertificate("classpath:org/springframework/boot/ssl/pem/key1.crt")
			.withPrivateKey("classpath:org/springframework/boot/ssl/pem/key1.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, null, "test-alias", "keysecret", true);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
	}

	@Test
	void shouldVerifyKeysIfEnabledAndCertificateChainIsUsed() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails
			.forCertificate("classpath:org/springframework/boot/ssl/pem/key2-chain.crt")
			.withPrivateKey("classpath:org/springframework/boot/ssl/pem/key2.pem");
		PemSslStoreBundle bundle = new PemSslStoreBundle(keyStoreDetails, null, "test-alias", "keysecret", true);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "keysecret".toCharArray()));
	}

	@Test
	void shouldFailIfVerifyKeysIsEnabledAndKeysDontMatch() {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails
			.forCertificate("classpath:org/springframework/boot/ssl/pem/key2.crt")
			.withPrivateKey("classpath:org/springframework/boot/ssl/pem/key1.pem");
		assertThatIllegalStateException()
			.isThrownBy(() -> new PemSslStoreBundle(keyStoreDetails, null, null, null, true))
			.withMessageContaining("Private key matches none of the certificates");
	}

	private Consumer<KeyStore> storeContainingCert(String keyAlias) {
		return storeContainingCert(KeyStore.getDefaultType(), keyAlias);
	}

	private Consumer<KeyStore> storeContainingCert(String keyStoreType, String keyAlias) {
		return ThrowingConsumer.of((keyStore) -> {
			assertThat(keyStore).isNotNull();
			assertThat(keyStore.getType()).isEqualTo(keyStoreType);
			assertThat(keyStore.containsAlias(keyAlias)).isTrue();
			assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
			assertThat(keyStore.getKey(keyAlias, EMPTY_KEY_PASSWORD)).isNull();
		});
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias) {
		return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias) {
		return storeContainingCertAndKey(keyStoreType, keyAlias, EMPTY_KEY_PASSWORD);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias, char[] keyPassword) {
		return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias, keyPassword);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias, char[] keyPassword) {
		return ThrowingConsumer.of((keyStore) -> {
			assertThat(keyStore).isNotNull();
			assertThat(keyStore.getType()).isEqualTo(keyStoreType);
			assertThat(keyStore.containsAlias(keyAlias)).isTrue();
			assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
			assertThat(keyStore.getKey(keyAlias, keyPassword)).isNotNull();
		});
	}

}
