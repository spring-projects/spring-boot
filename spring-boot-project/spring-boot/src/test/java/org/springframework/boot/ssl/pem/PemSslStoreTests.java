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

package org.springframework.boot.ssl.pem;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PemSslStore}.
 *
 * @author Phillip Webb
 */
class PemSslStoreTests {

	@Test
	void withAliasReturnsStoreWithNewAlias() {
		List<X509Certificate> certificates = List.of(mock(X509Certificate.class));
		PrivateKey privateKey = mock(PrivateKey.class);
		PemSslStore store = PemSslStore.of("type", "alias", "secret", certificates, privateKey);
		assertThat(store.withAlias("newalias").alias()).isEqualTo("newalias");
	}

	@Test
	void withPasswordReturnsStoreWithNewPassword() {
		List<X509Certificate> certificates = List.of(mock(X509Certificate.class));
		PrivateKey privateKey = mock(PrivateKey.class);
		PemSslStore store = PemSslStore.of("type", "alias", "secret", certificates, privateKey);
		assertThat(store.withPassword("newsecret").password()).isEqualTo("newsecret");
	}

	@Test
	void ofWhenNullCertificatesThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PemSslStore.of(null, null, null, null, null))
			.withMessage("'certificates' must not be empty");
	}

	@Test
	void ofWhenEmptyCertificatesThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> PemSslStore.of(null, null, null, Collections.emptyList(), null))
			.withMessage("'certificates' must not be empty");
	}

	@Test
	void ofReturnsPemSslStore() {
		List<X509Certificate> certificates = List.of(mock(X509Certificate.class));
		PrivateKey privateKey = mock(PrivateKey.class);
		PemSslStore store = PemSslStore.of("type", "alias", "password", certificates, privateKey);
		assertThat(store.type()).isEqualTo("type");
		assertThat(store.alias()).isEqualTo("alias");
		assertThat(store.password()).isEqualTo("password");
		assertThat(store.certificates()).isEqualTo(certificates);
		assertThat(store.privateKey()).isEqualTo(privateKey);
	}

}
