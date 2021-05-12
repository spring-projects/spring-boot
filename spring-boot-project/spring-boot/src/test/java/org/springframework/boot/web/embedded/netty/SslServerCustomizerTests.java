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

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 * @author Raheela Aslam
 */
@SuppressWarnings("deprecation")
class SslServerCustomizerTests {

	@Test
	void keyStoreProviderIsUsedWhenCreatingKeyStore() throws Exception {
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
	void trustStoreProviderIsUsedWhenCreatingTrustStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setTrustStorePassword("password");
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException().isThrownBy(() -> customizer.getTrustManagerFactory(ssl, null))
				.withCauseInstanceOf(NoSuchProviderException.class)
				.withMessageContaining("com.example.TrustStoreProvider");
	}

	@Test
	void getKeyManagerFactoryWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException() throws Exception {
		Ssl ssl = new Ssl();
		SslServerCustomizer customizer = new SslServerCustomizer(ssl, null, null);
		assertThatIllegalStateException().isThrownBy(() -> customizer.getKeyManagerFactory(ssl, null))
				.withCauseInstanceOf(WebServerException.class).withMessageContaining("Could not load key store 'null'");
	}

}
