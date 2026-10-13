/*
 * Copyright 2012-present the original author or authors.
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
