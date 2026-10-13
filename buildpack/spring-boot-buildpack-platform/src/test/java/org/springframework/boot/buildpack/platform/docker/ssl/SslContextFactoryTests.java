/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslContextFactory}.
 *
 * @author Scott Frederick
 */
class SslContextFactoryTests {

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
	void createKeyStoreWithCertChain() throws IOException {
		this.fileWriter.writeFile("cert.pem", PemFileWriter.CERTIFICATE);
		this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_RSA_KEY);
		this.fileWriter.writeFile("ca.pem", PemFileWriter.CA_CERTIFICATE);
		SSLContext sslContext = new SslContextFactory().forDirectory(this.fileWriter.getTempDir().toString());
		assertThat(sslContext).isNotNull();
	}

}
