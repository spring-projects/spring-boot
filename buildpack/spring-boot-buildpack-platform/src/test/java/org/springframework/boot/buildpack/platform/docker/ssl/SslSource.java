/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility to compare SSL source code.
 *
 * @author Phillip Webb
 */
final class SslSource {

	private static final Path BUILDPACK_LOCATION = Path
		.of("src/main/java/org/springframework/boot/buildpack/platform/docker/ssl");

	private static final Path SPRINGBOOT_LOCATION = Path
		.of("../../core/spring-boot/src/main/java/org/springframework/boot/ssl/pem");

	private SslSource() {
	}

	static String loadBuildpackVersion(String name) throws IOException {
		return load(BUILDPACK_LOCATION.resolve(name));
	}

	static String loadSpringBootVersion(String name) throws IOException {
		return load(SPRINGBOOT_LOCATION.resolve(name));
	}

	private static String load(Path path) throws IOException {
		String code = Files.readString(path);
		int firstBrace = code.indexOf("{");
		int lastBrace = code.lastIndexOf("}");
		return code.substring(firstBrace, lastBrace + 1);
	}

}
