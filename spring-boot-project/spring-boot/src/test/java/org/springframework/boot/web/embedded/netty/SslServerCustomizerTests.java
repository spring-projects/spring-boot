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

package org.springframework.boot.web.embedded.netty;

import java.security.NoSuchProviderException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.test.MockPkcs11Security;
import org.springframework.boot.web.embedded.test.MockPkcs11SecurityProvider;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProviderFactory;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 * @author Raheela Aslam
 * @author Cyril Dangerville
 * @author Scott Frederick
 */
@SuppressWarnings("deprecation")
@MockPkcs11Security
class SslServerCustomizerTests {

	@Test
	void keyStoreProviderIsUsedWhenCreatingKeyStore() {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getKeyManagerFactory(ssl, SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(NoSuchProviderException.class)
			.withMessageContaining("com.example.KeyStoreProvider");
	}

	@Test
	void trustStoreProviderIsUsedWhenCreatingTrustStore() {
		Ssl ssl = new Ssl();
		ssl.setTrustStorePassword("password");
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getTrustManagerFactory(SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(NoSuchProviderException.class)
			.withMessageContaining("com.example.TrustStoreProvider");
	}

	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsException() {
		Ssl ssl = new Ssl();
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getKeyManagerFactory(ssl, SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("KeyStore location must not be empty or null");
	}

	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsException() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getKeyManagerFactory(ssl, SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("must be empty or null for PKCS11 key stores");
	}

	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStorePassword("1234");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatNoException()
			.isThrownBy(() -> customizer.getKeyManagerFactory(ssl, SslStoreProviderFactory.from(ssl)));
	}

}
