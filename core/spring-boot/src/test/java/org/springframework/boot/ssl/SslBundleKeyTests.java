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
import java.security.cert.Certificate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslBundleKey}.
 *
 * @author Phillip Webb
 * @author Benedict
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
	void getKeyManagerFactoryWhenHasAliasNotInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(false);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
			.withMessage("Keystore does not contain alias 'alias'");
	}

	@Test
	void getKeyManagerFactoryWhenHasAliasNotDeterminedInStoreThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willThrow(KeyStoreException.class);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
			.withMessage("Could not validate keystore alias 'alias'");
	}

	@Test
	void assertContainsAliasWhenAliasIsNotKeyEntryThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(true);
		given(keyStore.isKeyEntry("alias")).willReturn(false);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
			.withMessage("Keystore alias 'alias' is not a key entry");
	}

	@Test
	void assertContainsAliasWhenAliasHasNoCertificateChainThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(true);
		given(keyStore.isKeyEntry("alias")).willReturn(true);
		given(keyStore.getCertificateChain("alias")).willReturn(null);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
			.withMessage("Keystore alias 'alias' does not have a certificate chain");
	}

	@Test
	void assertContainsAliasWhenAliasHasEmptyCertificateChainThrowsException() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(true);
		given(keyStore.isKeyEntry("alias")).willReturn(true);
		given(keyStore.getCertificateChain("alias")).willReturn(new Certificate[0]);
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatIllegalStateException().isThrownBy(() -> key.assertContainsAlias(keyStore))
			.withMessage("Keystore alias 'alias' does not have a certificate chain");
	}

	@Test
	void assertContainsAliasWhenAliasIsValidKeyEntryDoesNotThrow() throws Exception {
		KeyStore keyStore = mock(KeyStore.class);
		given(keyStore.containsAlias("alias")).willReturn(true);
		given(keyStore.isKeyEntry("alias")).willReturn(true);
		given(keyStore.getCertificateChain("alias")).willReturn(new Certificate[] { mock(Certificate.class) });
		SslBundleKey key = SslBundleKey.of("secret", "alias");
		assertThatNoException().isThrownBy(() -> key.assertContainsAlias(keyStore));
	}

}
