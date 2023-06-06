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

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesSslBundle}.
 *
 * @author Scott Frederick
 */
class PropertiesSslBundleTests {

	@Test
	void pemPropertiesAreMappedToSslBundle() {
		PemSslBundleProperties properties = new PemSslBundleProperties();
		properties.getKey().setAlias("alias");
		properties.getKey().setPassword("secret");
		properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
		properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
		properties.getKeystore().setCertificate("cert1.pem");
		properties.getKeystore().setPrivateKey("key1.pem");
		properties.getKeystore().setPrivateKeyPassword("keysecret1");
		properties.getKeystore().setType("PKCS12");
		properties.getTruststore().setCertificate("cert2.pem");
		properties.getTruststore().setPrivateKey("key2.pem");
		properties.getTruststore().setPrivateKeyPassword("keysecret2");
		properties.getTruststore().setType("JKS");
		SslBundle sslBundle = PropertiesSslBundle.get(properties);
		assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
		assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
		assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
		assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
		assertThat(sslBundle.getStores()).isNotNull();
		assertThat(sslBundle.getStores()).extracting("keyStoreDetails")
			.extracting("certificate", "privateKey", "privateKeyPassword", "type")
			.containsExactly("cert1.pem", "key1.pem", "keysecret1", "PKCS12");
		assertThat(sslBundle.getStores()).extracting("trustStoreDetails")
			.extracting("certificate", "privateKey", "privateKeyPassword", "type")
			.containsExactly("cert2.pem", "key2.pem", "keysecret2", "JKS");
	}

	@Test
	void jksPropertiesAreMappedToSslBundle() {
		JksSslBundleProperties properties = new JksSslBundleProperties();
		properties.getKey().setAlias("alias");
		properties.getKey().setPassword("secret");
		properties.getOptions().setCiphers(Set.of("cipher1", "cipher2", "cipher3"));
		properties.getOptions().setEnabledProtocols(Set.of("protocol1", "protocol2"));
		properties.getKeystore().setLocation("cert1.p12");
		properties.getKeystore().setPassword("secret1");
		properties.getKeystore().setProvider("provider1");
		properties.getKeystore().setType("JKS");
		properties.getTruststore().setLocation("cert2.jks");
		properties.getTruststore().setPassword("secret2");
		properties.getTruststore().setProvider("provider2");
		properties.getTruststore().setType("PKCS12");
		SslBundle sslBundle = PropertiesSslBundle.get(properties);
		assertThat(sslBundle.getKey().getAlias()).isEqualTo("alias");
		assertThat(sslBundle.getKey().getPassword()).isEqualTo("secret");
		assertThat(sslBundle.getOptions().getCiphers()).containsExactlyInAnyOrder("cipher1", "cipher2", "cipher3");
		assertThat(sslBundle.getOptions().getEnabledProtocols()).containsExactlyInAnyOrder("protocol1", "protocol2");
		assertThat(sslBundle.getStores()).isNotNull();
		assertThat(sslBundle.getStores()).extracting("keyStoreDetails")
			.extracting("location", "password", "provider", "type")
			.containsExactly("cert1.p12", "secret1", "provider1", "JKS");
		assertThat(sslBundle.getStores()).extracting("trustStoreDetails")
			.extracting("location", "password", "provider", "type")
			.containsExactly("cert2.jks", "secret2", "provider2", "PKCS12");
	}

}
