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
		.of("../../spring-boot/src/main/java/org/springframework/boot/ssl/pem");

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
