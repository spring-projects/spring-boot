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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.test.MockPkcs11Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JavaKeyStoreSslStoreProvider}.
 *
 * @author Scott Frederick
 */
@MockPkcs11Security
class JavaKeyStoreSslStoreProviderTests {

	@Test
	void fromSslWhenNullReturnsNull() {
		assertThat(JavaKeyStoreSslStoreProvider.from(null)).isNull();
	}

	@Test
	void fromSslWhenDisabledReturnsNull() {
		Ssl ssl = new Ssl();
		ssl.setEnabled(false);
		assertThat(JavaKeyStoreSslStoreProvider.from(ssl)).isNull();
	}

	@Test
	void getKeyStoreWithNoLocationThrowsException() {
		Ssl ssl = new Ssl();
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThatIllegalStateException().isThrownBy(storeProvider::getKeyStore)
			.withMessageContaining("KeyStore location must not be empty or null");
	}

	@Test
	void getKeyStoreWithTypePKCS11AndLocationThrowsException() {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("test.jks");
		ssl.setKeyStoreType("PKCS11");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThatIllegalStateException().isThrownBy(storeProvider::getKeyStore)
			.withMessageContaining("KeyStore location is 'test.jks', but must be empty or null for PKCS11 key stores");
	}

	@Test
	void getKeyStoreWithLocationReturnsKeyStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), "JKS", "test-alias", "password");
	}

	@Test
	void getTrustStoreWithLocationsReturnsTrustStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setTrustStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getTrustStore(), "JKS", "test-alias", "password");
	}

	@Test
	void getKeyStoreWithTypeUsesType() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyStoreType("PKCS12");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), "PKCS12", "test-alias", "password");
	}

	@Test
	void getTrustStoreWithTypeUsesType() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setTrustStore("classpath:test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setTrustStoreType("PKCS12");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getTrustStore(), "PKCS12", "test-alias", "password");
	}

	@Test
	void getKeyStoreWithProviderUsesProvider() {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThatExceptionOfType(NoSuchProviderException.class).isThrownBy(storeProvider::getKeyStore)
			.withMessageContaining("com.example.KeyStoreProvider");
	}

	@Test
	void getTrustStoreWithProviderUsesProvider() {
		Ssl ssl = new Ssl();
		ssl.setTrustStore("classpath:test.jks");
		ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
		SslStoreProvider storeProvider = JavaKeyStoreSslStoreProvider.from(ssl);
		assertThatExceptionOfType(NoSuchProviderException.class).isThrownBy(storeProvider::getTrustStore)
			.withMessageContaining("com.example.TrustStoreProvider");
	}

	private void assertStoreContainsCertAndKey(KeyStore keyStore, String keyStoreType, String keyAlias,
			String keyPassword) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		assertThat(keyStore).isNotNull();
		assertThat(keyStore.getType()).isEqualTo(keyStoreType);
		assertThat(keyStore.containsAlias(keyAlias)).isTrue();
		assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
		assertThat(keyStore.getKey(keyAlias, keyPassword.toCharArray())).isNotNull();
	}

}
