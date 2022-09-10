/*
 * Copyright 2012-2019 the original author or authors.
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
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.KeyManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.netty.MockPkcs11SecurityProvider;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.test.util.ReflectionTestUtils;

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
class SslBuilderCustomizerTests {

	private static final Provider PKCS11_PROVIDER = new MockPkcs11SecurityProvider();

	@BeforeAll
	static void beforeAllTests() {
		/*
		 * Add the mock Java security provider for PKCS#11-related unit tests.
		 *
		 */
		Security.addProvider(PKCS11_PROVIDER);
	}

	@AfterAll
	static void afterAllTests() {
		// Remove the provider previously added in setup()
		Security.removeProvider(PKCS11_PROVIDER.getName());
	}

	@Test
	void getKeyManagersWhenAliasIsNullShouldNotDecorate() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		KeyManager[] keyManagers = ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null);
		Class<?> name = Class.forName(
				"org.springframework.boot.web.embedded.undertow.SslBuilderCustomizer$ConfigurableAliasKeyManager");
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
				.isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null))
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
				.isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getTrustManagers", ssl, null))
				.withCauseInstanceOf(NoSuchProviderException.class)
				.withMessageContaining("com.example.TrustStoreProvider");
	}

	/**
	 * Null/undefined keystore is invalid unless keystore type is PKCS11.
	 */
	@Test
	void getKeyManagersWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsWebServerException() throws Exception {
		Ssl ssl = new Ssl();
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatIllegalStateException()
				.isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null))
				.withCauseInstanceOf(WebServerException.class).withMessageContaining("Could not load key store 'null'");
	}

	/**
	 * No keystore path should be defined if keystore type is PKCS#11.
	 */
	@Test
	void configureSslWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsIllegalArgumentException() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(PKCS11_PROVIDER.getName());
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		assertThatIllegalStateException()
				.isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null))
				.withCauseInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("Input keystore location is not valid for keystore type 'PKCS11'");
	}

	@Test
	void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(PKCS11_PROVIDER.getName());
		ssl.setKeyStorePassword("1234");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080, InetAddress.getLocalHost(), ssl, null);
		// Loading the KeyManagerFactory should be successful
		assertThatNoException()
				.isThrownBy(() -> ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null));
	}

}
