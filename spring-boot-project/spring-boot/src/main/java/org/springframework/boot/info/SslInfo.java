/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.info;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerSslBundle;

import static org.springframework.boot.info.SslInfo.CertificateInfo.Validity.Status.EXPIRED;
import static org.springframework.boot.info.SslInfo.CertificateInfo.Validity.Status.NOT_YET_VALID;
import static org.springframework.boot.info.SslInfo.CertificateInfo.Validity.Status.VALID;
import static org.springframework.boot.info.SslInfo.CertificateInfo.Validity.Status.WILL_EXPIRE_SOON;

/**
 * Information about the certificates that the application uses.
 *
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
public class SslInfo {

	private final List<Bundle> bundles;

	private final Duration certificateValidityThreshold;

	public SslInfo(Ssl ssl, SslBundles sslBundles, Duration certificateValidityThreshold) {
		List<Bundle> bundles = new ArrayList<>();
		for (Entry<String, SslBundle> entry : sslBundles.getBundles().entrySet()) {
			bundles.add(new Bundle(entry.getKey(), entry.getValue()));
		}
		if (ssl.getBundle() == null) {
			// TODO: WebServerSslBundle.get is called at multiple places
			//  (i.e.: in AbstractConfigurableWebServerFactory#getSslBundle)
			//  so this is a duplicate, can we create one instance and reuse it
			//  (e.g.: a bean) or integrate it with SslBundles
			//  that would make this block unnecessary?
			bundles.add(new Bundle("webServerSslBundle", WebServerSslBundle.get(ssl, sslBundles)));
		}
		this.bundles = Collections.unmodifiableList(bundles);
		this.certificateValidityThreshold = certificateValidityThreshold;
	}

	public List<Bundle> getBundles() {
		return this.bundles;
	}

	public class Bundle {

		private final String name;

		private final List<CertificateChain> certificateChains;

		private Bundle(String name, SslBundle sslBundle) {
			this.name = name;
			this.certificateChains = createCertificateChains(sslBundle.getStores().getKeyStore());
		}

		public String getName() {
			return this.name;
		}

		public List<CertificateChain> getCertificateChains() {
			return this.certificateChains;
		}

		private List<CertificateChain> createCertificateChains(KeyStore keyStore) {
			try {
				return Collections.list(keyStore.aliases())
					.stream()
					.map(alias -> new CertificateChain(alias, getCertificates(alias, keyStore)))
					.toList();
			}
			catch (KeyStoreException e) {
				return List.of();
			}
		}

		private List<Certificate> getCertificates(String alias, KeyStore keyStore) {
			try {
				return List.of(keyStore.getCertificateChain(alias));
			}
			catch (KeyStoreException e) {
				return List.of();
			}
		}

	}

	public class CertificateChain {

		private final String alias;

		private final List<CertificateInfo> certificates;

		CertificateChain(String alias, List<Certificate> certificates) {
			this.alias = alias;
			this.certificates = certificates.stream().map(CertificateInfo::new).toList();
		}

		public String getAlias() {
			return this.alias;
		}

		public List<CertificateInfo> getCertificates() {
			return this.certificates;
		}

	}

	public class CertificateInfo {

		private final X509Certificate certificate;

		private CertificateInfo(Certificate certificate) {
			// TODO: Is supporting X509Certificate enough? (I _assume_ yes)
			if (certificate instanceof X509Certificate x509Certificate) {
				this.certificate = x509Certificate;
			}
			else {
				this.certificate = null;
			}
		}

		public String getSubject() {
			return this.certificate != null ? this.certificate.getSubjectX500Principal().getName() : null;
		}

		public String getIssuer() {
			return this.certificate != null ? this.certificate.getIssuerX500Principal().getName() : null;
		}

		public String getSerialNumber() {
			return this.certificate != null ? this.certificate.getSerialNumber().toString(16) : null;
		}

		public String getVersion() {
			return this.certificate != null ? "V" + this.certificate.getVersion() : null;
		}

		public String getSignatureAlgorithmName() {
			return this.certificate != null ? this.certificate.getSigAlgName() : null;
		}

		public Instant getValidityStarts() {
			return this.certificate != null ? this.certificate.getNotBefore().toInstant() : null;
		}

		public Instant getValidityEnds() {
			return this.certificate != null ? this.certificate.getNotAfter().toInstant() : null;
		}

		public Validity getValidity() {
			try {
				if (this.certificate != null) {
					this.certificate.checkValidity();
					if (isCloseToBeExpired(this.certificate, SslInfo.this.certificateValidityThreshold)) {
						return new Validity(WILL_EXPIRE_SOON, "Certificate will expire within threshold (%s) at %s"
							.formatted(SslInfo.this.certificateValidityThreshold, this.getValidityEnds()));
					}
					else {
						return new Validity(VALID, null);
					}
				}
				else {
					return null;
				}
			}
			catch (CertificateNotYetValidException exception) {
				return new Validity(NOT_YET_VALID, "Not valid before %s".formatted(this.getValidityStarts()));
			}
			catch (CertificateExpiredException exception) {
				return new Validity(EXPIRED, "Not valid after %s".formatted(this.getValidityEnds()));
			}
		}

		private boolean isCloseToBeExpired(X509Certificate certificate, Duration certificateValidityThreshold) {
			Instant shouldBeValidAt = Instant.now().plus(certificateValidityThreshold);
			Instant expiresAt = certificate.getNotAfter().toInstant();
			return shouldBeValidAt.isAfter(expiresAt);
		}

		public static class Validity {

			private final Status status;

			private final String message;

			Validity(Status status, String message) {
				this.status = status;
				this.message = message;
			}

			public Status getStatus() {
				return this.status;
			}

			public String getMessage() {
				return this.message;
			}

			public enum Status {

				VALID, NOT_YET_VALID, EXPIRED, WILL_EXPIRE_SOON

			}

		}

	}

}
