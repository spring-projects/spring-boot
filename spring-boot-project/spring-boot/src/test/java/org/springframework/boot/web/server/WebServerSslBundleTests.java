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

import java.io.InputStream;
import java.security.KeyStore;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.embedded.test.MockPkcs11Security;
import org.springframework.boot.web.embedded.test.MockPkcs11SecurityProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServerSslBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@MockPkcs11Security
class WebServerSslBundleTests {

	@Test
	void whenSslDisabledThrowsException() {
		Ssl ssl = new Ssl();
		ssl.setEnabled(false);
		assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
			.withMessage("SSL is not enabled");
	}

	@Test
	void whenFromJksProperties() {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.p12");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStore("classpath:test.p12");
		ssl.setTrustStorePassword("secret");
		ssl.setTrustStoreType("PKCS12");
		ssl.setKeyPassword("password");
		ssl.setKeyAlias("alias");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setProtocol("TestProtocol");
		SslBundle bundle = WebServerSslBundle.get(ssl);
		assertThat(bundle).isNotNull();
		assertThat(bundle.getProtocol()).isEqualTo("TestProtocol");
		SslBundleKey key = bundle.getKey();
		assertThat(key.getPassword()).isEqualTo("password");
		assertThat(key.getAlias()).isEqualTo("alias");
		SslStoreBundle stores = bundle.getStores();
		assertThat(stores.getKeyStorePassword()).isEqualTo("secret");
		assertThat(stores.getKeyStore()).isNotNull();
		assertThat(stores.getTrustStore()).isNotNull();
		SslOptions options = bundle.getOptions();
		assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
		assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
	}

	@Test
	void whenFromJksPropertiesWithPkcs11StoreType() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
			.withMessageContaining("must be empty or null for PKCS11 hardware key stores");
	}

	@Test
	void whenFromPemProperties() {
		Ssl ssl = new Ssl();
		ssl.setCertificate("classpath:test-cert.pem");
		ssl.setCertificatePrivateKey("classpath:test-key.pem");
		ssl.setTrustCertificate("classpath:test-cert-chain.pem");
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStoreType("PKCS12");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setProtocol("TLSv1.1");
		SslBundle bundle = WebServerSslBundle.get(ssl);
		assertThat(bundle).isNotNull();
		SslBundleKey key = bundle.getKey();
		assertThat(key.getAlias()).isNull();
		assertThat(key.getPassword()).isEqualTo("password");
		SslStoreBundle stores = bundle.getStores();
		assertThat(stores.getKeyStorePassword()).isNull();
		assertThat(stores.getKeyStore()).isNotNull();
		assertThat(stores.getTrustStore()).isNotNull();
		SslOptions options = bundle.getOptions();
		assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
		assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
	}

	@Test
	@Deprecated(since = "3.1.0", forRemoval = true)
	@SuppressWarnings("removal")
	void whenFromCustomSslStoreProvider() throws Exception {
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		KeyStore keyStore = loadStore();
		given(sslStoreProvider.getKeyStore()).willReturn(keyStore);
		given(sslStoreProvider.getTrustStore()).willReturn(keyStore);
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS12");
		ssl.setTrustStoreType("PKCS12");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(Ssl.ClientAuth.NONE);
		ssl.setCiphers(new String[] { "ONE", "TWO", "THREE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });
		ssl.setProtocol("TLSv1.1");
		SslBundle bundle = WebServerSslBundle.get(ssl, null, sslStoreProvider);
		assertThat(bundle).isNotNull();
		SslBundleKey key = bundle.getKey();
		assertThat(key.getPassword()).isEqualTo("password");
		assertThat(key.getAlias()).isNull();
		SslStoreBundle stores = bundle.getStores();
		assertThat(stores.getKeyStore()).isNotNull();
		assertThat(stores.getTrustStore()).isNotNull();
		SslOptions options = bundle.getOptions();
		assertThat(options.getCiphers()).containsExactly("ONE", "TWO", "THREE");
		assertThat(options.getEnabledProtocols()).containsExactly("TLSv1.1", "TLSv1.2");
	}

	@Test
	void whenMissingPropertiesThrowsException() {
		Ssl ssl = new Ssl();
		assertThatIllegalStateException().isThrownBy(() -> WebServerSslBundle.get(ssl))
			.withMessageContaining("SSL is enabled but no trust material is configured");
	}

	private KeyStore loadStore() throws Exception {
		Resource resource = new ClassPathResource("test.p12");
		try (InputStream stream = resource.getInputStream()) {
			KeyStore keyStore = KeyStore.getInstance("PKCS12");
			keyStore.load(stream, "secret".toCharArray());
			return keyStore;
		}
	}

}
