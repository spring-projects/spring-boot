/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.elasticsearch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.Ssl;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link ElasticsearchConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link ElasticsearchContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ElasticsearchContainer, ElasticsearchConnectionDetails> {

	private static final int DEFAULT_PORT = 9200;

	@Override
	protected ElasticsearchConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ElasticsearchContainer> source) {
		return new ElasticsearchContainerConnectionDetails(source);
	}

	/**
	 * {@link ElasticsearchConnectionDetails} backed by a
	 * {@link ContainerConnectionSource}.
	 */
	private static final class ElasticsearchContainerConnectionDetails
			extends ContainerConnectionDetails<ElasticsearchContainer> implements ElasticsearchConnectionDetails {

		private volatile SslBundle sslBundle;

		private ElasticsearchContainerConnectionDetails(ContainerConnectionSource<ElasticsearchContainer> source) {
			super(source);
		}

		@Override
		public String getUsername() {
			return "elastic";
		}

		@Override
		public String getPassword() {
			return getContainer().getEnvMap().get("ELASTIC_PASSWORD");
		}

		@Override
		public List<Node> getNodes() {
			String host = getContainer().getHost();
			Integer port = getContainer().getMappedPort(DEFAULT_PORT);
			return List.of(new Node(host, port, (getSslBundle() != null) ? Protocol.HTTPS : Protocol.HTTP,
					getUsername(), getPassword()));
		}

		@Override
		public SslBundle getSslBundle() {
			if (this.sslBundle != null) {
				return this.sslBundle;
			}
			SslBundle sslBundle = super.getSslBundle();
			if (sslBundle != null) {
				this.sslBundle = sslBundle;
				return sslBundle;
			}
			if (hasAnnotation(Ssl.class)) {
				byte[] caCertificate = getContainer().caCertAsBytes().orElse(null);
				if (caCertificate != null) {
					KeyStore trustStore = createTrustStore(caCertificate);
					sslBundle = createSslBundleWithTrustStore(trustStore);
					this.sslBundle = sslBundle;
					return sslBundle;
				}
			}
			return null;
		}

		private SslBundle createSslBundleWithTrustStore(KeyStore trustStore) {
			return SslBundle.of(new SslStoreBundle() {
				@Override
				public KeyStore getKeyStore() {
					return null;
				}

				@Override
				public String getKeyStorePassword() {
					return null;
				}

				@Override
				public KeyStore getTrustStore() {
					return trustStore;
				}
			});
		}

		private KeyStore createTrustStore(byte[] caCertificate) {
			try {
				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(null, null);
				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				Certificate certificate = certFactory.generateCertificate(new ByteArrayInputStream(caCertificate));
				keyStore.setCertificateEntry("ca", certificate);
				return keyStore;
			}
			catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException ex) {
				throw new IllegalStateException("Failed to create keystore from CA certificate", ex);
			}
		}

	}

}
