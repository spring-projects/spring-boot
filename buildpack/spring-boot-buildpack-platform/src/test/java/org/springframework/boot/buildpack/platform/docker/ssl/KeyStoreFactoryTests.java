/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KeyStoreFactory}.
 *
 * @author Scott Frederick
 */
class KeyStoreFactoryTests {

	private PemFileWriter fileWriter;

	@BeforeEach
	void setUp() throws IOException {
		this.fileWriter = new PemFileWriter();
	}

	@AfterEach
	void tearDown() throws IOException {
		this.fileWriter.cleanup();
	}

	@Test
	void createKeyStoreWithCertChain()
			throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
		Path certPath = this.fileWriter.writeFile("cert.pem", PemFileWriter.CA_CERTIFICATE, PemFileWriter.CERTIFICATE);
		KeyStore keyStore = KeyStoreFactory.create(certPath, null, "test-alias");
		assertThat(keyStore.containsAlias("test-alias-0")).isTrue();
		assertThat(keyStore.getCertificate("test-alias-0")).isNotNull();
		assertThat(keyStore.getKey("test-alias-0", new char[] {})).isNull();
		assertThat(keyStore.containsAlias("test-alias-1")).isTrue();
		assertThat(keyStore.getCertificate("test-alias-1")).isNotNull();
		assertThat(keyStore.getKey("test-alias-1", new char[] {})).isNull();
	}

	@Test
	void createKeyStoreWithCertChainAndRsaPrivateKey()
			throws IOException, KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {
		Path certPath = this.fileWriter.writeFile("cert.pem", PemFileWriter.CA_CERTIFICATE, PemFileWriter.CERTIFICATE);
		Path keyPath = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_RSA_KEY);
		KeyStore keyStore = KeyStoreFactory.create(certPath, keyPath, "test-alias");
		assertThat(keyStore.containsAlias("test-alias")).isTrue();
		assertThat(keyStore.getCertificate("test-alias")).isNotNull();
		assertThat(keyStore.getKey("test-alias", new char[] {})).isNotNull();
	}

}
