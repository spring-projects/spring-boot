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

import java.nio.file.Path;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.CertificateSelector;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreDetails;
import org.springframework.util.StringUtils;

/**
 * {@link SslBundleProperties} for directories containing PEM-encoded certificates and
 * private keys.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 * @see PemDirectorySslStoreBundle
 */
public class PemDirectorySslBundleProperties extends SslBundleProperties {

	/**
	 * Directory containing the certificates and private keys.
	 */
	private String directory;

	/**
	 * Password used to decrypt an encrypted private key.
	 */
	private String privateKeyPassword;

	/**
	 * File extension of the certificates, e.g. '.crt'.
	 */
	private String certificateExtension = ".crt";

	/**
	 * File extension of the keys, e.g. '.key'.
	 */
	private String keyExtension = ".key";

	/**
	 * Certificate selection strategy.
	 */
	private CertificateSelection certificateSelection = CertificateSelection.NEWEST_NOT_BEFORE;

	/**
	 * Keystore properties.
	 */
	private final Store keystore = new Store();

	/**
	 * Truststore properties.
	 */
	private final Store truststore = new Store();

	/**
	 * Whether to verify that the private key matches the public key in the certificate.
	 */
	private boolean verifyKeys = false;

	public String getDirectory() {
		return this.directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getPrivateKeyPassword() {
		return this.privateKeyPassword;
	}

	public void setPrivateKeyPassword(String privateKeyPassword) {
		this.privateKeyPassword = privateKeyPassword;
	}

	public String getCertificateExtension() {
		return this.certificateExtension;
	}

	public void setCertificateExtension(String certificateExtension) {
		this.certificateExtension = certificateExtension;
	}

	public String getKeyExtension() {
		return this.keyExtension;
	}

	public void setKeyExtension(String keyExtension) {
		this.keyExtension = keyExtension;
	}

	public CertificateSelection getCertificateSelection() {
		return this.certificateSelection;
	}

	public void setCertificateSelection(CertificateSelection certificateSelection) {
		this.certificateSelection = certificateSelection;
	}

	public boolean isVerifyKeys() {
		return this.verifyKeys;
	}

	public void setVerifyKeys(boolean verifyKeys) {
		this.verifyKeys = verifyKeys;
	}

	public Store getKeystore() {
		return this.keystore;
	}

	public Store getTruststore() {
		return this.truststore;
	}

	void validate() {
		if (!StringUtils.hasLength(this.directory)) {
			throw new InvalidConfigurationPropertyValueException("spring.ssl.bundle.pemdir.*.directory", this.directory,
					"Must not be empty");
		}
	}

	PemDirectorySslStoreDetails toDetails() {
		return new PemDirectorySslStoreDetails(Path.of(getDirectory()), getKeystore().getType(),
				getTruststore().getType(), getPrivateKeyPassword(), getKey().getAlias(), isVerifyKeys());
	}

	/**
	 * Certificate selection strategy.
	 */
	public enum CertificateSelection {

		/**
		 * Selects the certificate with the longest 'Not After' field (which is usually
		 * the longest usable certificate).
		 */
		LONGEST_LIFETIME {
			@Override
			CertificateSelector getCertificateSelector() {
				return CertificateSelector.maximumNotAfter();
			}
		},
		/**
		 * Selects the certificate with the maximum 'Not Before' field (which is usually
		 * the most recently created certificate).
		 */
		NEWEST_NOT_BEFORE {
			@Override
			CertificateSelector getCertificateSelector() {
				return CertificateSelector.maximumNotBefore();
			}
		},

		/**
		 * Selects the certificate which has been created most recently.
		 */
		NEWEST_FILE {
			@Override
			CertificateSelector getCertificateSelector() {
				return CertificateSelector.newestFile();
			}
		};

		abstract CertificateSelector getCertificateSelector();

	}

	/**
	 * Store properties.
	 */
	public static class Store {

		/**
		 * Type of the store to create, e.g. JKS.
		 */
		private String type;

		public String getType() {
			return this.type;
		}

		public void setType(String type) {
			this.type = type;
		}

	}

}
