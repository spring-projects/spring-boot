/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultSslManagerBundle}.
 *
 * @author Phillip Webb
 */
class DefaultSslManagerBundleTests {

	private KeyManagerFactory keyManagerFactory = mock(KeyManagerFactory.class);

	private TrustManagerFactory trustManagerFactory = mock(TrustManagerFactory.class);

	@Test
	void getKeyManagerFactoryWhenStoreBundleIsNull() throws Exception {
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(null, SslBundleKey.NONE);
		KeyManagerFactory result = bundle.getKeyManagerFactory();
		assertThat(result).isNotNull();
		then(this.keyManagerFactory).should().init(null, null);
	}

	@Test
	void getKeyManagerFactoryWhenKeyIsNull() throws Exception {
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(SslStoreBundle.NONE, null);
		KeyManagerFactory result = bundle.getKeyManagerFactory();
		assertThat(result).isSameAs(this.keyManagerFactory);
		then(this.keyManagerFactory).should().init(null, null);
	}

	@Test
	void getKeyManagerFactoryWhenHasKeyAliasReturnsWrapped() {
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(null, SslBundleKey.of("secret", "alias"));
		KeyManagerFactory result = bundle.getKeyManagerFactory();
		assertThat(result).isInstanceOf(AliasKeyManagerFactory.class);
	}

	@Test
	void getKeyManagerFactoryWhenHasKeyPassword() throws Exception {
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(null, SslBundleKey.of("secret"));
		KeyManagerFactory result = bundle.getKeyManagerFactory();
		assertThat(result).isSameAs(this.keyManagerFactory);
		then(this.keyManagerFactory).should().init(null, "secret".toCharArray());
	}

	@Test
	void getKeyManagerFactoryWhenHasKeyStorePassword() throws Exception {
		SslStoreBundle storeBundle = SslStoreBundle.of(null, "secret", null);
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(storeBundle, null);
		KeyManagerFactory result = bundle.getKeyManagerFactory();
		assertThat(result).isSameAs(this.keyManagerFactory);
		then(this.keyManagerFactory).should().init(null, "secret".toCharArray());
	}

	@Test
	void getKeyManagerFactoryWhenHasAliasNotInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(false);
		SslStoreBundle storeBundle = SslStoreBundle.of(keyStore, null, null);
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(storeBundle,
				SslBundleKey.of("secret", "alias"));
		assertThatIllegalStateException().isThrownBy(bundle::getKeyManagerFactory)
			.withMessage("Keystore does not contain alias 'alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasAliasNotDeterminedInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willThrow(KeyStoreException.class);
		SslStoreBundle storeBundle = SslStoreBundle.of(keyStore, null, null);
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(storeBundle,
				SslBundleKey.of("secret", "alias"));
		assertThatIllegalStateException().isThrownBy(bundle::getKeyManagerFactory)
			.withMessage("Could not determine if keystore contains alias 'alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasStore() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		SslStoreBundle storeBundle = SslStoreBundle.of(keyStore, null, null);
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(storeBundle, null);
		KeyManagerFactory result = bundle.getKeyManagerFactory();
		assertThat(result).isSameAs(this.keyManagerFactory);
		then(this.keyManagerFactory).should().init(keyStore, null);
	}

	@Test
	void getTrustManagerFactoryWhenStoreBundleIsNull() throws Exception {
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(null, null);
		TrustManagerFactory result = bundle.getTrustManagerFactory();
		assertThat(result).isSameAs(this.trustManagerFactory);
		then(this.trustManagerFactory).should().init((KeyStore) null);
	}

	@Test
	void getTrustManagerFactoryWhenHasStore() throws Exception {
		KeyStore trustStore = mock(KeyStore.class);
		SslStoreBundle storeBundle = SslStoreBundle.of(null, null, trustStore);
		DefaultSslManagerBundle bundle = new TestDefaultSslManagerBundle(storeBundle, null);
		TrustManagerFactory result = bundle.getTrustManagerFactory();
		assertThat(result).isSameAs(this.trustManagerFactory);
		then(this.trustManagerFactory).should().init(trustStore);
	}

	/**
	 * Test version of {@link DefaultSslManagerBundle}.
	 */
	class TestDefaultSslManagerBundle extends DefaultSslManagerBundle {

		TestDefaultSslManagerBundle(SslStoreBundle storeBundle, SslBundleKey key) {
			super(storeBundle, key);
		}

		@Override
		protected KeyManagerFactory getKeyManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
			return DefaultSslManagerBundleTests.this.keyManagerFactory;
		}

		@Override
		protected TrustManagerFactory getTrustManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
			return DefaultSslManagerBundleTests.this.trustManagerFactory;
		}

	}

}
