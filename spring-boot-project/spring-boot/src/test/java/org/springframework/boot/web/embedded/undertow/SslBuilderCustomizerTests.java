/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link SslBuilderCustomizer}
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 */
public class SslBuilderCustomizerTests {

	@Test
	public void getKeyManagersWhenAliasIsNullShouldNotDecorate() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080,
				InetAddress.getLocalHost(), ssl, null);
		KeyManager[] keyManagers = ReflectionTestUtils.invokeMethod(customizer,
				"getKeyManagers", ssl, null);
		Class<?> name = Class.forName("org.springframework.boot.web.embedded.undertow"
				+ ".SslBuilderCustomizer$ConfigurableAliasKeyManager");
		assertThat(keyManagers[0]).isNotInstanceOf(name);
	}

	@Test
	public void keyStoreProviderIsUsedWhenCreatingKeyStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyPassword("password");
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStoreProvider("com.example.KeyStoreProvider");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080,
				InetAddress.getLocalHost(), ssl, null);
		try {
			ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null);
			fail();
		}
		catch (IllegalStateException ex) {
			Throwable cause = ex.getCause();
			assertThat(cause).isInstanceOf(NoSuchProviderException.class);
			assertThat(cause).hasMessageContaining("com.example.KeyStoreProvider");
		}
	}

	@Test
	public void trustStoreProviderIsUsedWhenCreatingTrustStore() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setTrustStorePassword("password");
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStoreProvider("com.example.TrustStoreProvider");
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080,
				InetAddress.getLocalHost(), ssl, null);
		try {
			ReflectionTestUtils.invokeMethod(customizer, "getTrustManagers", ssl, null);
			fail();
		}
		catch (IllegalStateException ex) {
			Throwable cause = ex.getCause();
			assertThat(cause).isInstanceOf(NoSuchProviderException.class);
			assertThat(cause).hasMessageContaining("com.example.TrustStoreProvider");
		}
	}

	@Test
	public void getKeyManagersWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException()
			throws Exception {
		Ssl ssl = new Ssl();
		SslBuilderCustomizer customizer = new SslBuilderCustomizer(8080,
				InetAddress.getLocalHost(), ssl, null);
		try {
			ReflectionTestUtils.invokeMethod(customizer, "getKeyManagers", ssl, null);
			fail();
		}
		catch (IllegalStateException ex) {
			Throwable cause = ex.getCause();
			assertThat(cause).isInstanceOf(WebServerException.class);
			assertThat(cause).hasMessageContaining("Could not load key store 'null'");
		}
	}

}
