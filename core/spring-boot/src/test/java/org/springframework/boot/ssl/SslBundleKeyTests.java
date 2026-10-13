/*
 * Copyright 2012-present the original author or authors.
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
			.withMessage("Could not determine if keystore contains alias 'alias'");
	}

}
