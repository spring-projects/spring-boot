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

package org.springframework.boot.ssl.jks;

import java.security.KeyStore;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.embedded.test.MockPkcs11Security;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JksSslStoreBundle}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@MockPkcs11Security
class JksSslStoreBundleTests {

	@Test
	void whenNullStores() {
		JksSslStoreDetails keyStoreDetails = null;
		JksSslStoreDetails trustStoreDetails = null;
		JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void whenStoresHaveNoValues() {
		JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation(null);
		JksSslStoreDetails trustStoreDetails = JksSslStoreDetails.forLocation(null);
		JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).isNull();
		assertThat(bundle.getKeyStorePassword()).isNull();
		assertThat(bundle.getTrustStore()).isNull();
	}

	@Test
	void whenTypePKCS11AndLocationThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails("PKCS11", null, "test.jks", null);
			JksSslStoreDetails trustStoreDetails = null;
			new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		})
			.withMessageContaining(
					"Unable to create key store: Location is 'test.jks', but must be empty or null for PKCS11 hardware key stores");
	}

	@Test
	void whenHasKeyStoreLocation() {
		JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation("classpath:test.jks")
			.withPassword("secret");
		JksSslStoreDetails trustStoreDetails = null;
		JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("test-alias", "password"));
	}

	@Test
	void getTrustStoreWithLocations() {
		JksSslStoreDetails keyStoreDetails = null;
		JksSslStoreDetails trustStoreDetails = JksSslStoreDetails.forLocation("classpath:test.jks")
			.withPassword("secret");
		JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("test-alias", "password"));
	}

	@Test
	void whenHasKeyStoreType() {
		JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails("jks", null, "classpath:test.jks", "secret");
		JksSslStoreDetails trustStoreDetails = null;
		JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getKeyStore()).satisfies(storeContainingCertAndKey("jks", "test-alias", "password"));
	}

	@Test
	void whenHasTrustStoreType() {
		JksSslStoreDetails keyStoreDetails = null;
		JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails("jks", null, "classpath:test.jks", "secret");
		JksSslStoreBundle bundle = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		assertThat(bundle.getTrustStore()).satisfies(storeContainingCertAndKey("jks", "test-alias", "password"));
	}

	@Test
	void whenHasKeyStoreProvider() {
		assertThatIllegalStateException().isThrownBy(() -> {
			JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(null, "com.example.KeyStoreProvider",
					"classpath:test.jks", "secret");
			JksSslStoreDetails trustStoreDetails = null;
			new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		}).withMessageContaining("com.example.KeyStoreProvider");
	}

	@Test
	void whenHasTrustStoreProvider() {
		assertThatIllegalStateException().isThrownBy(() -> {
			JksSslStoreDetails keyStoreDetails = null;
			JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(null, "com.example.KeyStoreProvider",
					"classpath:test.jks", "secret");
			new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		}).withMessageContaining("com.example.KeyStoreProvider");
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyAlias, String keyPassword) {
		return storeContainingCertAndKey(KeyStore.getDefaultType(), keyAlias, keyPassword);
	}

	private Consumer<KeyStore> storeContainingCertAndKey(String keyStoreType, String keyAlias, String keyPassword) {
		return ThrowingConsumer.of((keyStore) -> {
			assertThat(keyStore).isNotNull();
			assertThat(keyStore.getType()).isEqualTo(keyStoreType);
			assertThat(keyStore.containsAlias(keyAlias)).isTrue();
			assertThat(keyStore.getCertificate(keyAlias)).isNotNull();
			assertThat(keyStore.getKey(keyAlias, keyPassword.toCharArray())).isNotNull();
		});
	}

}
