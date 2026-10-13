/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AliasKeyManagerFactory}.
 *
 * @author Phillip Webb
 */
class AliasKeyManagerFactoryTests {

	@Test
	void chooseEngineServerAliasReturnsAlias() throws Exception {
		KeyManagerFactory delegate = mock(KeyManagerFactory.class);
		given(delegate.getKeyManagers()).willReturn(new KeyManager[] { mock(X509ExtendedKeyManager.class) });
		AliasKeyManagerFactory factory = new AliasKeyManagerFactory(delegate, "test-alias",
				KeyManagerFactory.getDefaultAlgorithm());
		factory.init(null, null);
		KeyManager[] keyManagers = factory.getKeyManagers();
		X509ExtendedKeyManager x509KeyManager = (X509ExtendedKeyManager) Arrays.stream(keyManagers)
			.filter(X509ExtendedKeyManager.class::isInstance)
			.findAny()
			.get();
		String chosenAlias = x509KeyManager.chooseEngineServerAlias(null, null, null);
		assertThat(chosenAlias).isEqualTo("test-alias");
	}

}
