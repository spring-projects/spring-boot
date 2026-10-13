/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FixedTrustManagerFactory}.
 *
 * @author Moritz Halbritter
 */
class FixedTrustManagerFactoryTests {

	@Test
	void shouldReturnTrustManagers() throws Exception {
		TrustManager trustManager1 = mock(TrustManager.class);
		TrustManager trustManager2 = mock(TrustManager.class);
		FixedTrustManagerFactory factory = FixedTrustManagerFactory.of(getDefaultTrustManagerFactory(), trustManager1,
				trustManager2);
		assertThat(factory.getTrustManagers()).containsExactly(trustManager1, trustManager2);
	}

	private static TrustManagerFactory getDefaultTrustManagerFactory() throws NoSuchAlgorithmException {
		return TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	}

}
