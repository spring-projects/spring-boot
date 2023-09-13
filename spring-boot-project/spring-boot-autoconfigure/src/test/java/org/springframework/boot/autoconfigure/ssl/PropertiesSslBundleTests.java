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

package org.springframework.boot.autoconfigure.ssl;

import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesSslBundle}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
class PropertiesSslBundleTests {

	@Test
	void pemPropertiesAreMappedToSslBundle() throws Exception {
		PemSslBundleProperties properties = new PemSslBundleProperties();
		properties.getKey().setAlias("alias");
		properties.getKey().setPassword("secret");
		properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
		properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
		properties.getKeystore().setCertificate("classpath:org/springframework/boot/autoconfigure/ssl/rsa-cert.pem");
		properties.getKeystore().setPrivateKey("classpath:org/springframework/boot/autoconfigure/ssl/rsa-key.pem");
		properties.getKeystore().setPrivateKeyPassword(null);
		properties.getKeystore().setType("PKCS12");
		properties.getTruststore()
			.setCertificate("classpath:org/springframework/boot/autoconfigure/ssl/ed25519-cert.pem");
		properties.getTruststore()
			.setPrivateKey("classpath:org/springframework/boot/autoconfigure/ssl/ed25519-key.pem");
		properties.getTruststore().setPrivateKeyPassword("secret");
		properties.getTruststore().setType("PKCS12");
		SslBundle sslBundle = PropertiesSslBundle.get(properties);
		assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
		assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
		assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
		assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
		assertThat(sslBundle.getStores()).isNotNull();
		Certificate certificate = sslBundle.getStores().getKeyStore().getCertificate("alias");
		assertThat(certificate).isNotNull();
		assertThat(certificate.getType()).isEqualTo("X.509");
		Key key = sslBundle.getStores().getKeyStore().getKey("alias", null);
		assertThat(key).isNotNull();
		assertThat(key.getAlgorithm()).isEqualTo("RSA");
		certificate = sslBundle.getStores().getTrustStore().getCertificate("alias");
		assertThat(certificate).isNotNull();
		assertThat(certificate.getType()).isEqualTo("X.509");
	}

	@Test
	void jksPropertiesAreMappedToSslBundle() {
		JksSslBundleProperties properties = new JksSslBundleProperties();
		properties.getKey().setAlias("alias");
		properties.getKey().setPassword("secret");
		properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
		properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
		properties.getKeystore().setPassword("secret");
		properties.getKeystore().setProvider("SUN");
		properties.getKeystore().setType("JKS");
		properties.getKeystore().setLocation("classpath:org/springframework/boot/autoconfigure/ssl/keystore.jks");
		properties.getTruststore().setPassword("secret");
		properties.getTruststore().setProvider("SUN");
		properties.getTruststore().setType("PKCS12");
		properties.getTruststore().setLocation("classpath:org/springframework/boot/autoconfigure/ssl/keystore.pkcs12");
		SslBundle sslBundle = PropertiesSslBundle.get(properties);
		assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
		assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
		assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
		assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
		assertThat(sslBundle.getStores()).isNotNull();
		assertThat(sslBundle.getStores()).extracting("keyStoreDetails")
			.extracting("location", "password", "provider", "type")
			.containsExactly("classpath:org/springframework/boot/autoconfigure/ssl/keystore.jks", "secret", "SUN",
					"JKS");
		KeyStore trustStore = sslBundle.getStores().getTrustStore();
		assertThat(trustStore.getType()).isEqualTo("PKCS12");
		assertThat(trustStore.getProvider().getName()).isEqualTo("SUN");
	}

}
