/*
 * Copyright 2012-2022 the original author or authors.
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
