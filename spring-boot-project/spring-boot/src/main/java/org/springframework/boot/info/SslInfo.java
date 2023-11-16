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
import java.util.Collections;
import java.util.List;

import org.springframework.boot.info.SslInfo.CertificateInfo.Validity.Status;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

/**
 * Information about the certificates that the application uses.
 *
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
public class SslInfo {

	private final SslBundles sslBundles;

	private final Duration certificateValidityWarningThreshold;

	public SslInfo(SslBundles sslBundles, Duration certificateValidityWarningThreshold) {
		this.sslBundles = sslBundles;
		this.certificateValidityWarningThreshold = certificateValidityWarningThreshold;
	}

	public List<Bundle> getBundles() {
		return this.sslBundles.getBundles()
			.entrySet()
			.stream()
			.map((entry) -> new Bundle(entry.getKey(), entry.getValue()))
			.toList();
	}

	public final class Bundle {

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
					.map((alias) -> new CertificateChain(alias, getCertificates(alias, keyStore)))
					.toList();
			}
			catch (KeyStoreException ex) {
				return Collections.emptyList();
			}
		}

		private List<Certificate> getCertificates(String alias, KeyStore keyStore) {
			try {
				Certificate[] certificateChain = keyStore.getCertificateChain(alias);
				return (certificateChain != null) ? List.of(certificateChain) : Collections.emptyList();
			}
			catch (KeyStoreException ex) {
				return Collections.emptyList();
			}
		}

	}

	public final class CertificateChain {

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

	public final class CertificateInfo {

		private final X509Certificate certificate;

		private CertificateInfo(Certificate certificate) {
			if (certificate instanceof X509Certificate x509Certificate) {
				this.certificate = x509Certificate;
			}
			else {
				this.certificate = null;
			}
		}

		public String getSubject() {
			return (this.certificate != null) ? this.certificate.getSubjectX500Principal().getName() : null;
		}

		public String getIssuer() {
			return (this.certificate != null) ? this.certificate.getIssuerX500Principal().getName() : null;
		}

		public String getSerialNumber() {
			return (this.certificate != null) ? this.certificate.getSerialNumber().toString(16) : null;
		}

		public String getVersion() {
			return (this.certificate != null) ? "V" + this.certificate.getVersion() : null;
		}

		public String getSignatureAlgorithmName() {
			return (this.certificate != null) ? this.certificate.getSigAlgName() : null;
		}

		public Instant getValidityStarts() {
			return (this.certificate != null) ? this.certificate.getNotBefore().toInstant() : null;
		}

		public Instant getValidityEnds() {
			return (this.certificate != null) ? this.certificate.getNotAfter().toInstant() : null;
		}

		public Validity getValidity() {
			try {
				if (this.certificate != null) {
					this.certificate.checkValidity();
					if (isCloseToBeExpired(this.certificate, SslInfo.this.certificateValidityWarningThreshold)) {
						return new Validity(Status.WILL_EXPIRE_SOON,
								"Certificate will expire within threshold (%s) at %s".formatted(
										SslInfo.this.certificateValidityWarningThreshold, this.getValidityEnds()));
					}
					else {
						return new Validity(Status.VALID, null);
					}
				}
				else {
					return null;
				}
			}
			catch (CertificateNotYetValidException exception) {
				return new Validity(Status.NOT_YET_VALID, "Not valid before %s".formatted(this.getValidityStarts()));
			}
			catch (CertificateExpiredException exception) {
				return new Validity(Status.EXPIRED, "Not valid after %s".formatted(this.getValidityEnds()));
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

				/**
				 * The certificate is valid.
				 */
				VALID,

				/**
				 * The certificate's validity date range is in the future.
				 */
				NOT_YET_VALID,

				/**
				 * The certificate's validity date range is in the past.
				 */
				EXPIRED,

				/**
				 * The certificate is still valid but the end of its validity date range
				 * is within the defined threshold.
				 */
				WILL_EXPIRE_SOON

			}

		}

	}

}
