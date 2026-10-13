/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemCertificateParser}.
 *
 * @author Phillip Webb
 */
class PemCertificateParserTests {

	private static final String SOURCE = "PemCertificateParser.java";

	@Test
	void codeShouldMatchSpringBootSslPackage() throws IOException {
		String buildpackVersion = SslSource.loadBuildpackVersion(SOURCE);
		String springBootVersion = SslSource.loadSpringBootVersion(SOURCE);
		assertThat(buildpackVersion).isEqualTo(springBootVersion);
	}

}
