/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemPrivateKeyParser}.
 *
 * @author Phillip Webb
 */
class PemPrivateKeyParserTests {

	private static final String SOURCE = "PemPrivateKeyParser.java";

	@Test
	void codeShouldMatchSpringBootSslPackage() throws IOException {
		String buildpackVersion = SslSource.loadBuildpackVersion(SOURCE);
		String springBootVersion = SslSource.loadSpringBootVersion(SOURCE);
		assertThat(buildpackVersion).isEqualTo(springBootVersion);
	}

}
