/*
 * Copyright 2012-2025 the original author or authors.
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslManagerBundle}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SslManagerBundleTests {

	private final KeyManagerFactory keyManagerFactory = mock(KeyManagerFactory.class);

	private final TrustManagerFactory trustManagerFactory = mock(TrustManagerFactory.class);

	@Test
	void getKeyManagersDelegatesToFactory() {
		SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
		bundle.getKeyManagers();
		then(this.keyManagerFactory).should().getKeyManagers();
	}

	@Test
	void getTrustManagersDelegatesToFactory() {
		SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
		bundle.getTrustManagers();
		then(this.trustManagerFactory).should().getTrustManagers();
	}

	@Test
	void createSslContextCreatesInitializedSslContext() {
		SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
		SSLContext sslContext = bundle.createSslContext("TLS");
		assertThat(sslContext).isNotNull();
		assertThat(sslContext.getProtocol()).isEqualTo("TLS");
	}

	@Test
	void ofWhenKeyManagerFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> SslManagerBundle.of(null, this.trustManagerFactory))
			.withMessage("'keyManagerFactory' must not be null");
	}

	@Test
	void ofWhenTrustManagerFactoryIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> SslManagerBundle.of(this.keyManagerFactory, null))
			.withMessage("'trustManagerFactory' must not be null");
	}

	@Test
	void ofCreatesSslManagerBundle() {
		SslManagerBundle bundle = SslManagerBundle.of(this.keyManagerFactory, this.trustManagerFactory);
		assertThat(bundle.getKeyManagerFactory()).isSameAs(this.keyManagerFactory);
		assertThat(bundle.getTrustManagerFactory()).isSameAs(this.trustManagerFactory);
	}

	@Test
	void fromCreatesDefaultSslManagerBundle() {
		SslManagerBundle bundle = SslManagerBundle.from(SslStoreBundle.NONE, SslBundleKey.NONE);
		assertThat(bundle).isInstanceOf(DefaultSslManagerBundle.class);
	}

	@Test
	void shouldReturnTrustManagerFactory() {
		SslManagerBundle bundle = SslManagerBundle.from(this.trustManagerFactory);
		assertThat(bundle.getKeyManagerFactory()).isNotNull();
		assertThat(bundle.getTrustManagerFactory()).isSameAs(this.trustManagerFactory);
	}

	@Test
	void shouldReturnTrustManagers() {
		TrustManager trustManager1 = mock(TrustManager.class);
		TrustManager trustManager2 = mock(TrustManager.class);
		SslManagerBundle bundle = SslManagerBundle.from(trustManager1, trustManager2);
		assertThat(bundle.getKeyManagerFactory()).isNotNull();
		assertThat(bundle.getTrustManagerFactory().getTrustManagers()).containsExactly(trustManager1, trustManager2);
	}

}
