/*
 * Copyright 2012-present the original author or authors.
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
	@SuppressWarnings("NullAway") // Test null check
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
