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

package org.springframework.boot.web.embedded.undertow;

import java.net.InetAddress;
import java.security.NoSuchProviderException;

import javax.net.ssl.KeyManager;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.test.MockPkcs11Security;
import org.springframework.boot.web.embedded.test.MockPkcs11SecurityProvider;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProviderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SslBuilderCustomizer}
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Cyril Dangerville
 */
@MockPkcs11Security
class SslBuilderCustomizerTests {

	@Test
	void getKeyManagersWhenAliasIsNullShouldNotDecorate() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		KeyManager[] keyManagers = customizer.getKeyManagers(ssl, SslStoreProviderFactory.from(ssl));
		Class<?> name = Class
			.forName("org.springframework.boot.web.embedded.undertow.SslBuilderCustomizer$ConfigurableAliasKeyManager");
		assertThat(keyManagers[0]).isNotInstanceOf(name);
	}

	@Test
	void keyStoreProviderIsUsedWhenCreatingKeyStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getKeyManagers(ssl, SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(NoSuchProviderException.class)
			.withMessageContaining("com.example.KeyStoreProvider");
	}

	@Test
	void trustStoreProviderIsUsedWhenCreatingTrustStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setTrustStorePassword("password");
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getTrustManagers(SslStoreProviderFactory.from(ssl)))
			.withMessageContaining("com.example.TrustStoreProvider");
	}

	@Test
	void getKeyManagersWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsException() throws Exception {
		Ssl ssl = new Ssl();
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getKeyManagers(ssl, SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("KeyStore location must not be empty or null");
	}

	@Test
	void configureSslWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsException() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> customizer.getKeyManagers(ssl, SslStoreProviderFactory.from(ssl)))
			.withCauseInstanceOf(IllegalStateException.class)
			.withMessageContaining("must be empty or null for PKCS11 key stores");
	}

	@Test
	void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(MockPkcs11SecurityProvider.NAME);
		ssl.setKeyStorePassword("1234");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatNoException().isThrownBy(() -> customizer.getKeyManagers(ssl, SslStoreProviderFactory.from(ssl)));
	}

}
