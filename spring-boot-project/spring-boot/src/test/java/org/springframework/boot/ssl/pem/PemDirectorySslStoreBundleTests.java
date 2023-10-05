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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.CertificateMatcher;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.CertificateSelector;
import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.KeyLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PemDirectorySslStoreBundle}.
 *
 * @author Moritz Halbritter
 */
class PemDirectorySslStoreBundleTests {

	private static final Instant NOW = Instant.parse("2023-12-31T00:00:00Z");

	private final CertificateMatcher certificateMatcher = new SuffixCertificateMatcher(".crt");

	private final KeyLocator keyLocator = new SuffixKeyLocator(".crt", ".key");

	private final CertificateSelector certificateSelector = new CertificateSelectors.MaximumNotAfterCertificateSelector(
			Clock.fixed(NOW, ZoneId.of("UTC")));

	@Test
	void shouldLoadFromDirectory(@TempDir Path tempDir)
			throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
		storeFile(tempDir, "key1.crt", "1.crt");
		storeFile(tempDir, "key1.pem", "1.key");
		storeFile(tempDir, "key2.crt", "2.crt");
		storeFile(tempDir, "key2.pem", "2.key");
		PemDirectorySslStoreBundle bundle = createBundle(tempDir);
		assertHasKey(bundle.getKeyStore(), "alias");
		// This is the serial number of key2.crt
		assertHasCertificate(bundle.getKeyStore(), "alias", "506931258775469093758931777760969589521779563942");
		assertHasCertificate(bundle.getTrustStore(), "alias-0", "506931258775469093758931777760969589521779563942");
	}

	@Test
	void shouldFailIfGivenNonExistentDirectory() {
		assertThatThrownBy(() -> createBundle(Path.of("/this/path/does/not/exist")))
			.isInstanceOf(UncheckedIOException.class)
			.hasMessage("Failed to list files in directory '/this/path/does/not/exist'");
	}

	@Test
	void shouldFailIfGivenEmptyDirectory(@TempDir Path tempDir) {
		assertThatIllegalStateException().isThrownBy(() -> createBundle(tempDir))
			.withMessage("No certificate could be selected. Candidates: []");
	}

	@Test
	void shouldFailIfKeyIsMissing(@TempDir Path tempDir) throws IOException {
		storeFile(tempDir, "key1.crt", "1.crt");
		storeFile(tempDir, "key2.crt", "2.crt");
		assertThatIllegalStateException().isThrownBy(() -> createBundle(tempDir))
			.withMessageMatching("Key for certificate '.+' not found");
	}

	@Test
	void shouldFailIfNoCertificatesCanBeLoaded(@TempDir Path tempDir) throws IOException {
		Files.createFile(tempDir.resolve("1.crt"));
		assertThatIllegalStateException().isThrownBy(() -> createBundle(tempDir))
			.withMessageMatching("No certificates found in file '.+'");
	}

	private PemDirectorySslStoreBundle createBundle(Path tempDir) {
		return new PemDirectorySslStoreBundle(
				new PemDirectorySslStoreDetails(tempDir, null, null, null, "alias", false), this.certificateMatcher,
				this.keyLocator, this.certificateSelector);
	}

	private void assertHasCertificate(KeyStore keyStore, String alias, String serialNumber) throws KeyStoreException {
		Certificate certificate = keyStore.getCertificate(alias);
		assertThat(certificate).as("certificate").isNotNull();
		assertThat(certificate).isInstanceOf(X509Certificate.class);
		X509Certificate x509Certificate = (X509Certificate) certificate;
		assertThat(x509Certificate.getSerialNumber()).isEqualTo(serialNumber);
	}

	private static void storeFile(Path tempDir, String resourceName, String fileName) throws IOException {
		try (InputStream resourceStream = PemDirectorySslStoreBundleTests.class.getResourceAsStream(resourceName);
				OutputStream fileStream = Files.newOutputStream(tempDir.resolve(fileName))) {
			assertThat(resourceStream).isNotNull();
			resourceStream.transferTo(fileStream);
		}
	}

	private static void assertHasKey(KeyStore keyStore, String alias)
			throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
		Key key = keyStore.getKey(alias, null);
		assertThat(key).as("key").isNotNull();
	}

}
