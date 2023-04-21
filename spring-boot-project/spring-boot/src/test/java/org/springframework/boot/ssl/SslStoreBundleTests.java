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

package org.springframework.boot.ssl;

import java.security.KeyStore;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslStoreBundle}
 *
 * @author Phillip Webb
 */
class SslStoreBundleTests {

	@Test
	void noneReturnsEmptySslStoreBundle() {
		SslStoreBundle bundle = SslStoreBundle.NONE;
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void ofCreatesStoreBundle() {
		KeyStore keyStore = mock(KeyStore.class);
		String keyStorePassword = "secret";
		KeyStore trustStore = mock(KeyStore.class);
		SslStoreBundle bundle = SslStoreBundle.of(keyStore, keyStorePassword, trustStore);
		assertThat(bundle.getKeyStore()).isSameAs(keyStore);
		assertThat(bundle.getKeyStorePassword()).isEqualTo(keyStorePassword);
		assertThat(bundle.getTrustStore()).isSameAs(trustStore);
	}

}
