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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.security.auth.x500.X500Principal;

import org.springframework.boot.info.SslInfo.CertificateValidityInfo.Status;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.util.ObjectUtils;

/**
 * Information about the certificates that the application uses.
 *
 * @author Jonatan Ivanov
 * @author Joshua Chen
 * @since 3.4.0
 */
public class SslInfo {

	private final SslBundles sslBundles;

	private final Duration certificateValidityWarningThreshold;

	public SslInfo(SslBundles sslBundles, Duration certificateValidityWarningThreshold) {
		this.sslBundles = sslBundles;
		this.certificateValidityWarningThreshold = certificateValidityWarningThreshold;
	}

	public List<BundleInfo> getBundles() {
		return this.sslBundles.getBundleNames()
			.stream()
			.map((name) -> new BundleInfo(name, this.sslBundles.getBundle(name)))
			.toList();
	}

	/**
	 * Info about a single {@link SslBundle}.
	 */
	public final class BundleInfo {

		private final String name;

		private final List<CertificateChainInfo> certificateChains;

		private BundleInfo(String name, SslBundle sslBundle) {
			this.name = name;
			this.certificateChains = extractCertificateChains(sslBundle.getStores());
		}

		private List<CertificateChainInfo> extractCertificateChains(SslStoreBundle storeBundle) {
			if (storeBundle == null) {
				return Collections.emptyList();
			}
			List<CertificateChainInfo> certificateChains = new ArrayList<>();
			if (storeBundle.getKeyStore() != null) {
				certificateChains.addAll(extractCertificateChains(storeBundle.getKeyStore()));
			}
			if (storeBundle.getTrustStore() != null) {
				certificateChains.addAll(extractCertificateChains(storeBundle.getTrustStore()));
			}
			return certificateChains;
		}

		private List<CertificateChainInfo> extractCertificateChains(KeyStore keyStore) {
			if (keyStore == null) {
				return Collections.emptyList();
			}
			try {
				return Collections.list(keyStore.aliases())
					.stream()
					.map((alias) -> new CertificateChainInfo(keyStore, alias))
					.toList();
			}
			catch (KeyStoreException ex) {
				return Collections.emptyList();
			}
		}

		public String getName() {
			return this.name;
		}

		public List<CertificateChainInfo> getCertificateChains() {
			return this.certificateChains;
		}

	}

	/**
	 * Info about a single certificate chain.
	 */
	public final class CertificateChainInfo {

		private final String alias;

		private final Set<CertificateInfo> certificates;

		CertificateChainInfo(KeyStore keyStore, String alias) {
			this.alias = alias;
			this.certificates = extractCertificates(keyStore, alias);
		}

		private Set<CertificateInfo> extractCertificates(KeyStore keyStore, String alias) {
			try {
				Certificate[] certificates = keyStore.getCertificateChain(alias);
				Set<CertificateInfo> certificateInfos = new java.util.HashSet<>(!ObjectUtils.isEmpty(certificates)
						? Arrays.stream(certificates).map(CertificateInfo::new).collect(Collectors.toSet())
						: Collections.emptySet());
				if (keyStore.getCertificate(alias) != null) {
					certificateInfos.add(new CertificateInfo(keyStore.getCertificate(alias)));
				}
				return certificateInfos;
			}
			catch (KeyStoreException ex) {
				return Collections.emptySet();
			}
		}

		public String getAlias() {
			return this.alias;
		}

		public Set<CertificateInfo> getCertificates() {
			return this.certificates;
		}

	}

	/**
	 * Info about a certificate.
	 */
	public final class CertificateInfo {

		private final X509Certificate certificate;

		private CertificateInfo(Certificate certificate) {
			this.certificate = (certificate instanceof X509Certificate x509Certificate) ? x509Certificate : null;
		}

		public String getSubject() {
			return extract(X509Certificate::getSubjectX500Principal, X500Principal::getName);
		}

		public String getIssuer() {
			return extract(X509Certificate::getIssuerX500Principal, X500Principal::getName);
		}

		public String getSerialNumber() {
			return extract(X509Certificate::getSerialNumber, (serial) -> serial.toString(16));
		}

		public String getVersion() {
			return extract((certificate) -> "V" + certificate.getVersion());
		}

		public String getSignatureAlgorithmName() {
			return extract(X509Certificate::getSigAlgName);
		}

		public Instant getValidityStarts() {
			return extract(X509Certificate::getNotBefore, Date::toInstant);
		}

		public Instant getValidityEnds() {
			return extract(X509Certificate::getNotAfter, Date::toInstant);
		}

		public CertificateValidityInfo getValidity() {
			return extract((certificate) -> {
				Instant starts = getValidityStarts();
				Instant ends = getValidityEnds();
				Duration threshold = SslInfo.this.certificateValidityWarningThreshold;
				try {
					certificate.checkValidity();
					return (!isExpiringSoon(certificate, threshold)) ? CertificateValidityInfo.VALID
							: new CertificateValidityInfo(Status.WILL_EXPIRE_SOON,
									"Certificate will expire within threshold (%s) at %s", threshold, ends);
				}
				catch (CertificateNotYetValidException ex) {
					return new CertificateValidityInfo(Status.NOT_YET_VALID, "Not valid before %s", starts);
				}
				catch (CertificateExpiredException ex) {
					return new CertificateValidityInfo(Status.EXPIRED, "Not valid after %s", ends);
				}
			});
		}

		private boolean isExpiringSoon(X509Certificate certificate, Duration threshold) {
			Instant shouldBeValidAt = Instant.now().plus(threshold);
			Instant expiresAt = certificate.getNotAfter().toInstant();
			return shouldBeValidAt.isAfter(expiresAt);
		}

		private <V, R> R extract(Function<X509Certificate, V> valueExtractor, Function<V, R> resultExtractor) {
			return extract(valueExtractor.andThen(resultExtractor));
		}

		private <R> R extract(Function<X509Certificate, R> extractor) {
			return (this.certificate != null) ? extractor.apply(this.certificate) : null;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}
			return (this.certificate != null) ? this.certificate.equals(((CertificateInfo) other).certificate)
					: super.equals(other);
		}

		@Override
		public int hashCode() {
			return (this.certificate != null) ? this.certificate.hashCode() : super.hashCode();
		}

	}

	/**
	 * Certificate validity info.
	 */
	public static class CertificateValidityInfo {

		static final CertificateValidityInfo VALID = new CertificateValidityInfo(Status.VALID, null);

		private final Status status;

		private final String message;

		CertificateValidityInfo(Status status, String message, Object... messageArgs) {
			this.status = status;
			this.message = (message != null) ? message.formatted(messageArgs) : null;
		}

		public Status getStatus() {
			return this.status;
		}

		public String getMessage() {
			return this.message;
		}

		/**
		 * Validity Status.
		 */
		public enum Status {

			/**
			 * The certificate is valid.
			 */
			VALID(true),

			/**
			 * The certificate's validity date range is in the future.
			 */
			NOT_YET_VALID(false),

			/**
			 * The certificate's validity date range is in the past.
			 */
			EXPIRED(false),

			/**
			 * The certificate is still valid, but the end of its validity date range is
			 * within the defined threshold.
			 */
			WILL_EXPIRE_SOON(true);

			private final boolean valid;

			Status(boolean valid) {
				this.valid = valid;
			}

			public boolean isValid() {
				return this.valid;
			}

		}

	}

}
