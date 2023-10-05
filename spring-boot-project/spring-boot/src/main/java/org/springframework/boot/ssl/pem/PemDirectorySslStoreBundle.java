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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.util.Assert;

/**
 * A {@link SslStoreBundle} which uses a directory containing certificates and keys in PEM
 * encoding.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 * @see PemSslStoreBundle
 */
public class PemDirectorySslStoreBundle implements SslStoreBundle {

	private final SslStoreBundle delegate;

	/**
	 * Creates a new {@link PemDirectorySslStoreBundle}.
	 * @param details the {@link PemDirectorySslStoreDetails} to create the bundle
	 * @param certificateMatcher the strategy to find certificates
	 * @param keyLocator the strategy to find the key for a certificate
	 * @param certificateSelector the strategy to select a certificate
	 */
	public PemDirectorySslStoreBundle(PemDirectorySslStoreDetails details, CertificateMatcher certificateMatcher,
			KeyLocator keyLocator, CertificateSelector certificateSelector) {
		Assert.notNull(details, "details must not be null");
		Assert.notNull(certificateMatcher, "certificateMatcher must not be null");
		Assert.notNull(keyLocator, "keyLocator must not be null");
		Assert.notNull(certificateSelector, "certificateSelector must not be null");
		List<Path> files = listFiles(details.directory());
		List<Certificate> certificates = findCertificates(certificateMatcher, files);
		Certificate certificate = selectCertificate(certificateSelector, certificates);
		Path key = findKey(keyLocator, certificate, files);
		this.delegate = loadBundle(certificate.file(), key, details);
	}

	@Override
	public KeyStore getKeyStore() {
		return this.delegate.getKeyStore();
	}

	@Override
	public String getKeyStorePassword() {
		return this.delegate.getKeyStorePassword();
	}

	@Override
	public KeyStore getTrustStore() {
		return this.delegate.getTrustStore();
	}

	private static PemSslStoreBundle loadBundle(Path certificate, Path key, PemDirectorySslStoreDetails details) {
		String certificateContent = readContent(certificate);
		String keyContent = readContent(key);
		return new PemSslStoreBundle(
				new PemSslStoreDetails(details.keyStoreType(), certificateContent, keyContent,
						details.privateKeyPassword()),
				new PemSslStoreDetails(details.trustStoreType(), certificateContent, null), details.alias(), null,
				details.verifyKeys());
	}

	private static List<Path> listFiles(Path directory) {
		try (Stream<Path> fileStream = Files.list(directory)) {
			return fileStream.toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to list files in directory '%s'".formatted(directory), ex);
		}
	}

	private static Certificate selectCertificate(CertificateSelector certificateSelector,
			List<Certificate> certificates) {
		Certificate selected = certificateSelector.select(certificates);
		if (selected == null) {
			throw new IllegalStateException("No certificate could be selected. Candidates: %s".formatted(certificates));
		}
		return selected;
	}

	private static Path findKey(KeyLocator keyLocator, Certificate certificate, List<Path> files) {
		Path key = keyLocator.locate(certificate, files);
		if (key == null || !Files.exists(key)) {
			throw new IllegalStateException("Key for certificate '%s' not found".formatted(certificate.file()));
		}
		return key;
	}

	private static List<Certificate> findCertificates(CertificateMatcher certificateMatcher, List<Path> files) {
		List<Certificate> candidates = new ArrayList<>();
		for (Path file : files) {
			if (certificateMatcher.matches(file)) {
				String content = readContent(file);
				X509Certificate[] x509Certificates = PemCertificateParser.parse(content);
				if (x509Certificates == null || x509Certificates.length == 0) {
					throw new IllegalStateException("No certificates found in file '%s'".formatted(file));
				}
				// Always use the first certificate if it is a chain
				candidates.add(new Certificate(file, x509Certificates[0]));
			}
		}
		return candidates;
	}

	private static String readContent(Path file) {
		try {
			return Files.readString(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read content of file '%s'".formatted(file), ex);
		}
	}

	/**
	 * Certificate.
	 *
	 * @param file the certificate file
	 * @param certificate the parsed certificate
	 */
	public record Certificate(Path file, X509Certificate certificate) {
	}

	public interface KeyLocator {

		/**
		 * Locates the key belonging to the given {@code certificate}.
		 * @param certificate the certificate to locate a key for
		 * @param files the available files
		 * @return the path to the key, or {@code null}
		 */
		Path locate(Certificate certificate, Collection<Path> files);

		/**
		 * Creates a {@link KeyLocator} which selects keys based on the file extension.
		 * @param certificateExtension the extension of certificate files
		 * @param keyExtension the extension of key files
		 * @return the key locator
		 */
		static KeyLocator withExtension(String certificateExtension, String keyExtension) {
			return new SuffixKeyLocator(certificateExtension, keyExtension);
		}

	}

	public interface CertificateMatcher {

		/**
		 * Decides whether the given {@code file} is a certificate.
		 * @param file the file to decide on
		 * @return whether the file is a certificate
		 */
		boolean matches(Path file);

		/**
		 * Creates a {@link CertificateMatcher} which selects certificates based on the
		 * file extension.
		 * @param certificateExtension the extension of certificate files
		 * @return the certificate matcher
		 */
		static CertificateMatcher withExtension(String certificateExtension) {
			return new SuffixCertificateMatcher(certificateExtension);
		}

	}

	public interface CertificateSelector {

		/**
		 * Selects a certificate from the given {@code certificates}.
		 * @param certificates the certificates
		 * @return the selected certificate
		 */
		Certificate select(List<Certificate> certificates);

		/**
		 * Creates a {@link CertificateSelector} which selects the certificate with the
		 * maximum not after field.
		 * @return the certificate selector
		 */
		static CertificateSelector maximumNotAfter() {
			return new CertificateSelectors.MaximumNotAfterCertificateSelector();
		}

		/**
		 * Creates a {@link CertificateSelector} which selects the certificate with the
		 * maximum not before field.
		 * @return the certificate selector
		 */
		static CertificateSelector maximumNotBefore() {
			return new CertificateSelectors.MaximumNotBeforeCertificateSelector();
		}

		/**
		 * Creates a {@link CertificateSelector} which selects the certificate with the
		 * newest file creation date.
		 * @return the certificate selector
		 */
		static CertificateSelector newestFile() {
			return new CertificateSelectors.NewestFileCertificateSelector();
		}

	}

}
