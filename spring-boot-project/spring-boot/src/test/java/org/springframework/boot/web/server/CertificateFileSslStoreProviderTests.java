/*
 * Copyright 2012-2022 the original author or authors.
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
import java.security.UnrecoverableKeyException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CertificateFileSslStoreProvider}.
 *
 * @author Scott Frederick
 */
class CertificateFileSslStoreProviderTests {

	@Test
	void fromSslWhenNullReturnsNull() {
		assertThat(CertificateFileSslStoreProvider.from(null)).isNull();
	}

	@Test
	void fromSslWhenDisabledReturnsNull() {
		assertThat(CertificateFileSslStoreProvider.from(new Ssl())).isNull();
	}

	@Test
	void fromSslWithCertAndKeyReturnsStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "spring-boot-web");
		assertThat(storeProvider.getTrustStore()).isNull();
	}

	@Test
	void fromSslWithCertAndKeyAndTrustCertReturnsStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert.pem");
		SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "spring-boot-web");
		assertStoreContainsCert(storeProvider.getTrustStore(), KeyStore.getDefaultType(), "spring-boot-web-0");
	}

	@Test
	void fromSslWithCertAndKeyAndTrustCertAndTrustKeyReturnsStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert.pem");
		ssl.setTrustCertificatePrivateKey("classpath:test-key.pem");
		SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "spring-boot-web");
		assertStoreContainsCertAndKey(storeProvider.getTrustStore(), KeyStore.getDefaultType(), "spring-boot-web");
	}

	@Test
	void fromSslWithKeyAliasReturnsStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setKeyAlias("test-alias");
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert.pem");
		ssl.setTrustCertificatePrivateKey("classpath:test-key.pem");
		SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), KeyStore.getDefaultType(), "test-alias");
		assertStoreContainsCertAndKey(storeProvider.getTrustStore(), KeyStore.getDefaultType(), "test-alias");
	}

	@Test
	void fromSslWithStoreTypeReturnsStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStoreType("PKCS12");
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert.pem");
		ssl.setTrustCertificatePrivateKey("classpath:test-key.pem");
		SslStoreProvider storeProvider = CertificateFileSslStoreProvider.from(ssl);
		assertThat(storeProvider).isNotNull();
		assertStoreContainsCertAndKey(storeProvider.getKeyStore(), "PKCS12", "spring-boot-web");
		assertStoreContainsCertAndKey(storeProvider.getTrustStore(), "PKCS12", "spring-boot-web");
	}

	private void assertStoreContainsCertAndKey(KeyStore keyStore, String keyStoreType, String keyAlias)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		assertThat(keyStore).isNotNull();
		assertThat(keyStore.getType()).isEqualTo(keyStoreType);
		assertThat(keyStore.containsAlias(keyAlias)).isTrue();
		assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
		assertThat(keyStore.getKey(keyAlias, new char[] {})).isNotNull();
	}

	private void assertStoreContainsCert(KeyStore keyStore, String keyStoreType, String keyAlias)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		assertThat(keyStore).isNotNull();
		assertThat(keyStore.getType()).isEqualTo(keyStoreType);
		assertThat(keyStore.containsAlias(keyAlias)).isTrue();
		assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
		assertThat(keyStore.getKey(keyAlias, new char[] {})).isNull();
	}

}
