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

package org.springframework.boot.docker.compose.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ImageReference}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ImageReferenceTests {

	@Test
	void ofSimpleName() {
		ImageReference reference = ImageReference.of("ubuntu");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("docker.io/library/ubuntu");
	}

	@Test
	void ofLibrarySlashName() {
		ImageReference reference = ImageReference.of("library/ubuntu");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("docker.io/library/ubuntu");
	}

	@Test
	void ofSlashName() {
		ImageReference reference = ImageReference.of("adoptopenjdk/openjdk11");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("adoptopenjdk/openjdk11");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("docker.io/adoptopenjdk/openjdk11");
	}

	@Test
	void ofCustomDomain() {
		ImageReference reference = ImageReference.of("repo.example.com/java/jdk");
		assertThat(reference.getDomain()).isEqualTo("repo.example.com");
		assertThat(reference.getName()).isEqualTo("java/jdk");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("repo.example.com/java/jdk");
	}

	@Test
	void ofCustomDomainAndPort() {
		ImageReference reference = ImageReference.of("repo.example.com:8080/java/jdk");
		assertThat(reference.getDomain()).isEqualTo("repo.example.com:8080");
		assertThat(reference.getName()).isEqualTo("java/jdk");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("repo.example.com:8080/java/jdk");
	}

	@Test
	void ofLegacyDomain() {
		ImageReference reference = ImageReference.of("index.docker.io/ubuntu");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("docker.io/library/ubuntu");
	}

	@Test
	void ofNameAndTag() {
		ImageReference reference = ImageReference.of("ubuntu:bionic");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isEqualTo("bionic");
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("docker.io/library/ubuntu:bionic");
	}

	@Test
	void ofDomainPortAndTag() {
		ImageReference reference = ImageReference.of("repo.example.com:8080/library/ubuntu:v1");
		assertThat(reference.getDomain()).isEqualTo("repo.example.com:8080");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isEqualTo("v1");
		assertThat(reference.getDigest()).isNull();
		assertThat(reference).hasToString("repo.example.com:8080/library/ubuntu:v1");
	}

	@Test
	void ofNameAndDigest() {
		ImageReference reference = ImageReference
			.of("ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest())
			.isEqualTo("sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference).hasToString(
				"docker.io/library/ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
	}

	@Test
	void ofNameAndTagAndDigest() {
		ImageReference reference = ImageReference
			.of("ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isEqualTo("bionic");
		assertThat(reference.getDigest())
			.isEqualTo("sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference).hasToString(
				"docker.io/library/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
	}

	@Test
	void ofCustomDomainAndPortWithTag() {
		ImageReference reference = ImageReference
			.of("example.com:8080/canonical/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference.getDomain()).isEqualTo("example.com:8080");
		assertThat(reference.getName()).isEqualTo("canonical/ubuntu");
		assertThat(reference.getTag()).isEqualTo("bionic");
		assertThat(reference.getDigest())
			.isEqualTo("sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference).hasToString(
				"example.com:8080/canonical/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
	}

	@Test
	void ofWhenHasIllegalCharacter() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> ImageReference
				.of("registry.example.com/example/example-app:1.6.0-dev.2.uncommitted+wip.foo.c75795d"))
			.withMessageContaining("Unable to parse image reference");
	}

	@Test
	void equalsAndHashCode() {
		ImageReference r1 = ImageReference.of("ubuntu:bionic");
		ImageReference r2 = ImageReference.of("docker.io/library/ubuntu:bionic");
		ImageReference r3 = ImageReference.of("docker.io/library/ubuntu:latest");
		assertThat(r1).hasSameHashCodeAs(r2);
		assertThat(r1).isEqualTo(r1).isEqualTo(r2).isNotEqualTo(r3);
	}

}
