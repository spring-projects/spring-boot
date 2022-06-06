/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.config.SslConfigs;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.core.io.Resource;

/**
 * Spring Kafka SSL related configuration properties for Kafka clients.
 *
 * @author Chris Bono
 */
public class Ssl {

	/**
	 * Password of the private key in either key store key or key store file.
	 */
	private String keyPassword;

	/**
	 * Certificate chain in PEM format with a list of X.509 certificates.
	 */
	private String keyStoreCertificateChain;

	/**
	 * Private key in PEM format with PKCS#8 keys.
	 */
	private String keyStoreKey;

	/**
	 * Location of the key store file.
	 */
	private Resource keyStoreLocation;

	/**
	 * Store password for the key store file.
	 */
	private String keyStorePassword;

	/**
	 * Type of the key store.
	 */
	private String keyStoreType;

	/**
	 * Trusted certificates in PEM format with X.509 certificates.
	 */
	private String trustStoreCertificates;

	/**
	 * Location of the trust store file.
	 */
	private Resource trustStoreLocation;

	/**
	 * Store password for the trust store file.
	 */
	private String trustStorePassword;

	/**
	 * Type of the trust store.
	 */
	private String trustStoreType;

	/**
	 * SSL protocol to use.
	 */
	private String protocol;

	public String getKeyPassword() {
		return this.keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public String getKeyStoreCertificateChain() {
		return this.keyStoreCertificateChain;
	}

	public void setKeyStoreCertificateChain(String keyStoreCertificateChain) {
		this.keyStoreCertificateChain = keyStoreCertificateChain;
	}

	public String getKeyStoreKey() {
		return this.keyStoreKey;
	}

	public void setKeyStoreKey(String keyStoreKey) {
		this.keyStoreKey = keyStoreKey;
	}

	public Resource getKeyStoreLocation() {
		return this.keyStoreLocation;
	}

	public void setKeyStoreLocation(Resource keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}

	public String getKeyStorePassword() {
		return this.keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyStoreType() {
		return this.keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getTrustStoreCertificates() {
		return this.trustStoreCertificates;
	}

	public void setTrustStoreCertificates(String trustStoreCertificates) {
		this.trustStoreCertificates = trustStoreCertificates;
	}

	public Resource getTrustStoreLocation() {
		return this.trustStoreLocation;
	}

	public void setTrustStoreLocation(Resource trustStoreLocation) {
		this.trustStoreLocation = trustStoreLocation;
	}

	public String getTrustStorePassword() {
		return this.trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public String getTrustStoreType() {
		return this.trustStoreType;
	}

	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public Map<String, Object> buildProperties() {
		validate();
		PropertiesMap properties = new PropertiesMap();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this::getKeyPassword).to(properties.in(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
		map.from(this::getKeyStoreCertificateChain).to(properties.in(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG));
		map.from(this::getKeyStoreKey).to(properties.in(SslConfigs.SSL_KEYSTORE_KEY_CONFIG));
		map.from(this::getKeyStoreLocation).as(this::resourceToPath)
				.to(properties.in(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
		map.from(this::getKeyStorePassword).to(properties.in(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
		map.from(this::getKeyStoreType).to(properties.in(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG));
		map.from(this::getTrustStoreCertificates).to(properties.in(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG));
		map.from(this::getTrustStoreLocation).as(this::resourceToPath)
				.to(properties.in(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
		map.from(this::getTrustStorePassword).to(properties.in(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
		map.from(this::getTrustStoreType).to(properties.in(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
		map.from(this::getProtocol).to(properties.in(SslConfigs.SSL_PROTOCOL_CONFIG));
		return properties;
	}

	private void validate() {
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("spring.kafka.ssl.key-store-key", this.getKeyStoreKey());
			entries.put("spring.kafka.ssl.key-store-location", this.getKeyStoreLocation());
		});
		MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
			entries.put("spring.kafka.ssl.trust-store-certificates", this.getTrustStoreCertificates());
			entries.put("spring.kafka.ssl.trust-store-location", this.getTrustStoreLocation());
		});
	}

	private String resourceToPath(Resource resource) {
		try {
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Resource '" + resource + "' must be on a file system", ex);
		}
	}

}
