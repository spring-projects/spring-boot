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

package org.springframework.boot.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.info.SslInfo.BundleInfo;
import org.springframework.boot.info.SslInfo.CertificateChainInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo;
import org.springframework.boot.info.SslInfo.CertificateValidityInfo.Status;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslInfo}.
 *
 * @author Jonatan Ivanov
 */
class SslInfoTests {

	@Test
	@WithPackageResources("test.p12")
	void validCertificatesShouldProvideSslInfo() {
		SslInfo sslInfo = createSslInfo("classpath:test.p12");
		assertThat(sslInfo.getBundles()).hasSize(1);
		BundleInfo bundle = sslInfo.getBundles().get(0);
		assertThat(bundle.getName()).isEqualTo("test-0");
		assertThat(bundle.getCertificateChains()).hasSize(4);
		assertThat(bundle.getCertificateChains().get(0).getAlias()).isEqualTo("spring-boot");
		assertThat(bundle.getCertificateChains().get(0).getCertificates()).hasSize(1);
		assertThat(bundle.getCertificateChains().get(1).getAlias()).isEqualTo("test-alias");
		assertThat(bundle.getCertificateChains().get(1).getCertificates()).hasSize(1);
		assertThat(bundle.getCertificateChains().get(2).getAlias()).isEqualTo("spring-boot-cert");
		assertThat(bundle.getCertificateChains().get(2).getCertificates()).isEmpty();
		assertThat(bundle.getCertificateChains().get(3).getAlias()).isEqualTo("test-alias-cert");
		assertThat(bundle.getCertificateChains().get(3).getCertificates()).isEmpty();
		CertificateInfo cert1 = bundle.getCertificateChains().get(0).getCertificates().get(0);
		assertThat(cert1.getSubject()).isEqualTo("CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US");
		assertThat(cert1.getIssuer()).isEqualTo(cert1.getSubject());
		assertThat(cert1.getSerialNumber()).isNotEmpty();
		assertThat(cert1.getVersion()).isEqualTo("V3");
		assertThat(cert1.getSignatureAlgorithmName()).isEqualTo("SHA256withRSA");
		assertThat(cert1.getValidityStarts()).isInThePast();
		assertThat(cert1.getValidityEnds()).isInTheFuture();
		assertThat(cert1.getValidity()).isNotNull();
		assertThat(cert1.getValidity().getStatus()).isSameAs(Status.VALID);
		assertThat(cert1.getValidity().getMessage()).isNull();
		CertificateInfo cert2 = bundle.getCertificateChains().get(1).getCertificates().get(0);
		assertThat(cert2.getSubject()).isEqualTo("CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US");
		assertThat(cert2.getIssuer()).isEqualTo(cert2.getSubject());
		assertThat(cert2.getSerialNumber()).isNotEmpty();
		assertThat(cert2.getVersion()).isEqualTo("V3");
		assertThat(cert2.getSignatureAlgorithmName()).isEqualTo("SHA256withRSA");
		assertThat(cert2.getValidityStarts()).isInThePast();
		assertThat(cert2.getValidityEnds()).isInTheFuture();
		assertThat(cert2.getValidity()).isNotNull();
		assertThat(cert2.getValidity().getStatus()).isSameAs(Status.VALID);
		assertThat(cert2.getValidity().getMessage()).isNull();
	}

	@Test
	@WithPackageResources("test-not-yet-valid.p12")
	void notYetValidCertificateShouldProvideSslInfo() {
		SslInfo sslInfo = createSslInfo("classpath:test-not-yet-valid.p12");
		assertThat(sslInfo.getBundles()).hasSize(1);
		BundleInfo bundle = sslInfo.getBundles().get(0);
		assertThat(bundle.getName()).isEqualTo("test-0");
		assertThat(bundle.getCertificateChains()).hasSize(1);
		CertificateChainInfo certificateChain = bundle.getCertificateChains().get(0);
		assertThat(certificateChain.getAlias()).isEqualTo("spring-boot");
		List<CertificateInfo> certs = certificateChain.getCertificates();
		assertThat(certs).hasSize(1);
		CertificateInfo cert = certs.get(0);
		assertThat(cert.getSubject()).isEqualTo("CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US");
		assertThat(cert.getIssuer()).isEqualTo(cert.getSubject());
		assertThat(cert.getSerialNumber()).isNotEmpty();
		assertThat(cert.getVersion()).isEqualTo("V3");
		assertThat(cert.getSignatureAlgorithmName()).isEqualTo("SHA256withRSA");
		assertThat(cert.getValidityStarts()).isInTheFuture();
		assertThat(cert.getValidityEnds()).isInTheFuture();
		assertThat(cert.getValidity()).isNotNull();
		assertThat(cert.getValidity().getStatus()).isSameAs(Status.NOT_YET_VALID);
		assertThat(cert.getValidity().getMessage()).startsWith("Not valid before");
	}

	@Test
	@WithPackageResources("test-expired.p12")
	void expiredCertificateShouldProvideSslInfo() {
		SslInfo sslInfo = createSslInfo("classpath:test-expired.p12");
		assertThat(sslInfo.getBundles()).hasSize(1);
		BundleInfo bundle = sslInfo.getBundles().get(0);
		assertThat(bundle.getName()).isEqualTo("test-0");
		assertThat(bundle.getCertificateChains()).hasSize(1);
		CertificateChainInfo certificateChain = bundle.getCertificateChains().get(0);
		assertThat(certificateChain.getAlias()).isEqualTo("spring-boot");
		List<CertificateInfo> certs = certificateChain.getCertificates();
		assertThat(certs).hasSize(1);
		CertificateInfo cert = certs.get(0);
		assertThat(cert.getSubject()).isEqualTo("CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US");
		assertThat(cert.getIssuer()).isEqualTo(cert.getSubject());
		assertThat(cert.getSerialNumber()).isNotEmpty();
		assertThat(cert.getVersion()).isEqualTo("V3");
		assertThat(cert.getSignatureAlgorithmName()).isEqualTo("SHA256withRSA");
		assertThat(cert.getValidityStarts()).isInThePast();
		assertThat(cert.getValidityEnds()).isInThePast();
		assertThat(cert.getValidity()).isNotNull();
		assertThat(cert.getValidity().getStatus()).isSameAs(Status.EXPIRED);
		assertThat(cert.getValidity().getMessage()).startsWith("Not valid after");
	}

	@Test
	void soonToBeExpiredCertificateShouldProvideSslInfo(@TempDir Path tempDir)
			throws IOException, InterruptedException {
		Path keyStore = createKeyStore(tempDir);
		SslInfo sslInfo = createSslInfo(keyStore.toString());
		assertThat(sslInfo.getBundles()).hasSize(1);
		BundleInfo bundle = sslInfo.getBundles().get(0);
		assertThat(bundle.getName()).isEqualTo("test-0");
		assertThat(bundle.getCertificateChains()).hasSize(1);
		CertificateChainInfo certificateChain = bundle.getCertificateChains().get(0);
		assertThat(certificateChain.getAlias()).isEqualTo("spring-boot");
		List<CertificateInfo> certs = certificateChain.getCertificates();
		assertThat(certs).hasSize(1);
		CertificateInfo cert = certs.get(0);
		assertThat(cert.getSubject()).isEqualTo("CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US");
		assertThat(cert.getIssuer()).isEqualTo(cert.getSubject());
		assertThat(cert.getSerialNumber()).isNotEmpty();
		assertThat(cert.getVersion()).isEqualTo("V3");
		assertThat(cert.getSignatureAlgorithmName()).isNotEmpty();
		assertThat(cert.getValidityStarts()).isInThePast();
		assertThat(cert.getValidityEnds()).isInTheFuture();
		assertThat(cert.getValidity()).isNotNull();
		assertThat(cert.getValidity().getStatus()).isSameAs(Status.WILL_EXPIRE_SOON);
		assertThat(cert.getValidity().getMessage()).startsWith("Certificate will expire within threshold");
	}

	@Test
	@WithPackageResources({ "test.p12", "test-not-yet-valid.p12", "test-expired.p12" })
	void multipleBundlesShouldProvideSslInfo(@TempDir Path tempDir) throws IOException, InterruptedException {
		Path keyStore = createKeyStore(tempDir);
		SslInfo sslInfo = createSslInfo("classpath:test.p12", "classpath:test-not-yet-valid.p12",
				"classpath:test-expired.p12", keyStore.toString());
		assertThat(sslInfo.getBundles()).hasSize(4);
		assertThat(sslInfo.getBundles()).allSatisfy((bundle) -> assertThat(bundle.getName()).startsWith("test-"));
		List<CertificateInfo> certs = sslInfo.getBundles()
			.stream()
			.flatMap((bundle) -> bundle.getCertificateChains().stream())
			.flatMap((certificateChain) -> certificateChain.getCertificates().stream())
			.toList();
		assertThat(certs).hasSize(5);
		assertThat(certs).allSatisfy((cert) -> {
			assertThat(cert.getSubject()).isEqualTo("CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US");
			assertThat(cert.getIssuer()).isEqualTo(cert.getSubject());
			assertThat(cert.getSerialNumber()).isNotEmpty();
			assertThat(cert.getVersion()).isEqualTo("V3");
			assertThat(cert.getSignatureAlgorithmName()).isNotEmpty();
			assertThat(cert.getValidity()).isNotNull();
		});
		assertThat(certs).anySatisfy((cert) -> {
			assertThat(cert.getValidityStarts()).isInThePast();
			assertThat(cert.getValidityEnds()).isInTheFuture();
			assertThat(cert.getValidity()).isNotNull();
			assertThat(cert.getValidity().getStatus()).isSameAs(Status.VALID);
			assertThat(cert.getValidity().getMessage()).isNull();
		});
		assertThat(certs).satisfiesOnlyOnce((cert) -> {
			assertThat(cert.getValidityStarts()).isInTheFuture();
			assertThat(cert.getValidityEnds()).isInTheFuture();
			assertThat(cert.getValidity()).isNotNull();
			assertThat(cert.getValidity().getStatus()).isSameAs(Status.NOT_YET_VALID);
			assertThat(cert.getValidity().getMessage()).startsWith("Not valid before");
		});
		assertThat(certs).satisfiesOnlyOnce((cert) -> {
			assertThat(cert.getValidityStarts()).isInThePast();
			assertThat(cert.getValidityEnds()).isInThePast();
			assertThat(cert.getValidity()).isNotNull();
			assertThat(cert.getValidity().getStatus()).isSameAs(Status.EXPIRED);
			assertThat(cert.getValidity().getMessage()).startsWith("Not valid after");
		});
		assertThat(certs).satisfiesOnlyOnce((cert) -> {
			assertThat(cert.getValidityStarts()).isInThePast();
			assertThat(cert.getValidityEnds()).isInTheFuture();
			assertThat(cert.getValidity()).isNotNull();
			assertThat(cert.getValidity().getStatus()).isSameAs(Status.WILL_EXPIRE_SOON);
			assertThat(cert.getValidity().getMessage()).startsWith("Certificate will expire within threshold");
		});
	}

	@Test
	void nullKeyStore() {
		DefaultSslBundleRegistry sslBundleRegistry = new DefaultSslBundleRegistry();
		sslBundleRegistry.registerBundle("test", SslBundle.of(SslStoreBundle.NONE, SslBundleKey.NONE));
		SslInfo sslInfo = new SslInfo(sslBundleRegistry, Duration.ofDays(7));
		assertThat(sslInfo.getBundles()).hasSize(1);
		assertThat(sslInfo.getBundles().get(0).getCertificateChains()).isEmpty();
	}

	private SslInfo createSslInfo(String... locations) {
		DefaultSslBundleRegistry sslBundleRegistry = new DefaultSslBundleRegistry();
		for (int i = 0; i < locations.length; i++) {
			JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation(locations[i]).withPassword("secret");
			SslStoreBundle sslStoreBundle = new JksSslStoreBundle(keyStoreDetails, null);
			sslBundleRegistry.registerBundle("test-%d".formatted(i), SslBundle.of(sslStoreBundle));
		}
		return new SslInfo(sslBundleRegistry, Duration.ofDays(7));
	}

	private Path createKeyStore(Path directory) throws IOException, InterruptedException {
		Path keyStore = directory.resolve("test.p12");
		Process process = createProcessBuilder(keyStore).start();
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String out = reader.lines().collect(Collectors.joining("\n"));
				throw new RuntimeException("Unexpected exit code from keytool: %d\n%s".formatted(exitCode, out));
			}
		}
		return keyStore;
	}

	private ProcessBuilder createProcessBuilder(Path keystore) {
		// @formatter:off
		ProcessBuilder processBuilder = new ProcessBuilder(
				"keytool",
				"-genkeypair",
				"-storetype", "PKCS12",
				"-alias", "spring-boot",
				"-keyalg", "RSA",
				"-storepass", "secret",
				"-keypass", "secret",
				"-keystore", keystore.toString(),
				"-dname", "CN=localhost,OU=Spring,O=VMware,L=Palo Alto,ST=California,C=US",
				"-validity", "1",
				"-ext", "SAN=DNS:localhost,IP:::1,IP:127.0.0.1"
		);
		// @formatter:on
		processBuilder.redirectErrorStream(true);
		return processBuilder;
	}

}
