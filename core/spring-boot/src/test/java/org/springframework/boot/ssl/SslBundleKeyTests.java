/*
 * Copyright 2012-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslBundleKey}.
 *
 * @author Phillip Webb
 */
class SslBundleKeyTests {

	@Test
	void noneHasNoValues() {
		SslBundleKey keyReference = SslBundleKey.NONE;
		assertThat(keyReference.getPassword()).isNull();
		assertThat(keyReference.getAlias()).isNull();
	}

	@Test
	void ofCreatesWithPasswordSslKeyReference() {
		SslBundleKey keyReference = SslBundleKey.of("password");
		assertThat(keyReference.getPassword()).isEqualTo("password");
		assertThat(keyReference.getAlias()).isNull();
	}

	@Test
	void ofCreatesWithPasswordAndAliasSslKeyReference() {
		SslBundleKey keyReference = SslBundleKey.of("password", "alias");
		assertThat(keyReference.getPassword()).isEqualTo("password");
		assertThat(keyReference.getAlias()).isEqualTo("alias");
	}

	@Test
	void ofCreatesWithPasswordAndServerAliasAndClientAliasSslKeyReference() {
		SslBundleKey keyReference = SslBundleKey.of("password", "server-alias", "client-alias");
		assertThat(keyReference.getPassword()).isEqualTo("password");
		assertThat(keyReference.getServerAlias()).isEqualTo("server-alias");
		assertThat(keyReference.getClientAlias()).isEqualTo("client-alias");
	}

	@Test
	void getKeyManagerFactoryWhenHasAliasNotInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(false);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAliases(keyStore))
			.withMessage("Keystore does not contain server alias 'alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasAliasNotDeterminedInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willThrow(KeyStoreException.class);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAliases(keyStore))
			.withMessage("Could not determine if keystore contains server alias 'alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasServerAliasNotInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("server-alias")).willReturn(false);
		SslBundleKey key = SslBundleKey.of("secret", "server-alias", null);
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAliases(keyStore))
			.withMessage("Keystore does not contain server alias 'server-alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasServerAliasNotDeterminedInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("server-alias")).willThrow(KeyStoreException.class);
		SslBundleKey key = SslBundleKey.of("secret", "server-alias", null);
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAliases(keyStore))
			.withMessage("Could not determine if keystore contains server alias 'server-alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasClientAliasNotInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("client-alias")).willReturn(false);
		SslBundleKey key = SslBundleKey.of("secret", null, "client-alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAliases(keyStore))
			.withMessage("Keystore does not contain client alias 'client-alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasClientAliasNotDeterminedInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("client-alias")).willThrow(KeyStoreException.class);
		SslBundleKey key = SslBundleKey.of("secret", null, "client-alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAliases(keyStore))
			.withMessage("Could not determine if keystore contains client alias 'client-alias'");
	}
}
