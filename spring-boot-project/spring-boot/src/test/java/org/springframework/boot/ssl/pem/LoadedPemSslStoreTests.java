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

package org.springframework.boot.ssl.pem;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link LoadedPemSslStore}.
 *
 * @author Phillip Webb
 */
class LoadedPemSslStoreTests {

	@Test
	void certificatesAreLoadedLazily() {
		PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:missing-test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		LoadedPemSslStore store = new LoadedPemSslStore(details);
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
	}

	@Test
	void privateKeyIsLoadedLazily() {
		PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:test-cert.pem")
			.withPrivateKey("classpath:missing-test-key.pem");
		LoadedPemSslStore store = new LoadedPemSslStore(details);
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::privateKey);
	}

	@Test
	void withAliasIsLazy() {
		PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:missing-test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStore store = new LoadedPemSslStore(details).withAlias("alias");
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
	}

	@Test
	void withPasswordIsLazy() {
		PemSslStoreDetails details = PemSslStoreDetails.forCertificate("classpath:missing-test-cert.pem")
			.withPrivateKey("classpath:test-key.pem");
		PemSslStore store = new LoadedPemSslStore(details).withPassword("password");
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(store::certificates);
	}

}
