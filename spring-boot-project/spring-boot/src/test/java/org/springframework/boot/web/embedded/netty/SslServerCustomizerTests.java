/*
 * Copyright 2012-2021 the original author or authors.
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
import java.security.Provider;
import java.security.Security;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 * @author Raheela Aslam
 * @author Cyril Dangerville
 */
@SuppressWarnings("deprecation")
class SslServerCustomizerTests {

	private static final Provider PKCS11_PROVIDER = new MockPkcs11SecurityProvider();

	@BeforeAll
	static void setup() {
		/*
		 * Add the mock Java security provider for PKCS#11-related unit tests.
		 *
		 * For an integration test with an actual PKCS#11 library - SoftHSM - properly
		 * installed and configured on the system (inside a container), used via Java
		 * built-in SunPKCS11 provider, see the 'spring-boot-smoke-test-webflux-ssl'
		 * project in 'spring-boot-tests/spring-boot-smoke-tests' folder.
		 */
		Security.addProvider(PKCS11_PROVIDER);
	}

	@AfterAll
	static void shutdown() {
		// Remove the provider previously added in setup()
		Security.removeProvider(PKCS11_PROVIDER.getName());
	}

	@Test
	void keyStoreProviderIsUsedWhenCreatingKeyStore() {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null))
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
		assertThatIllegalStateException().isThrownBy(() -> customizer.getTrustManagerFactory(ssl, null))
				.withCauseInstanceOf(NoSuchProviderException.class)
				.withMessageContaining("com.example.TrustStoreProvider");
	}

	/**
	 * Null/undefined keystore is not valid unless keystore type is PKCS11.
	 */
	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsWebServerException() {
		Ssl ssl = new Ssl();
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null))
				.withCauseInstanceOf(WebServerException.class).withMessageContaining("Could not load key store 'null'");
	}

	/**
	 * No keystore path should be defined if keystore type is PKCS#11.
	 */
	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsIllegalArgumentException() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(PKCS11_PROVIDER.getName());
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("Input keystore location is not valid for keystore type 'PKCS11'");
	}

	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(PKCS11_PROVIDER.getName());
		ssl.setKeyStorePassword("1234");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		// Loading the KeyManagerFactory should be successful
		assertThatNoException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null));
	}

}
