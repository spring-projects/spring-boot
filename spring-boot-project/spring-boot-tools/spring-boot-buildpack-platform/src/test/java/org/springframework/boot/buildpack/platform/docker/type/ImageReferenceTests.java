/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ImageReference}.
 *
 * @author Phillip Webb
 */
class ImageReferenceTests {

	@Test
	void ofSimpleName() {
		ImageReference reference = ImageReference.of("ubuntu");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("docker.io/library/ubuntu");
	}

	@Test
	void ofLibrarySlashName() {
		ImageReference reference = ImageReference.of("library/ubuntu");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("docker.io/library/ubuntu");
	}

	@Test
	void ofSlashName() {
		ImageReference reference = ImageReference.of("adoptopenjdk/openjdk11");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("adoptopenjdk/openjdk11");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("docker.io/adoptopenjdk/openjdk11");
	}

	@Test
	void ofCustomDomain() {
		ImageReference reference = ImageReference.of("repo.example.com/java/jdk");
		assertThat(reference.getDomain()).isEqualTo("repo.example.com");
		assertThat(reference.getName()).isEqualTo("java/jdk");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("repo.example.com/java/jdk");
	}

	@Test
	void ofCustomDomainAndPort() {
		ImageReference reference = ImageReference.of("repo.example.com:8080/java/jdk");
		assertThat(reference.getDomain()).isEqualTo("repo.example.com:8080");
		assertThat(reference.getName()).isEqualTo("java/jdk");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("repo.example.com:8080/java/jdk");
	}

	@Test
	void ofLegacyDomain() {
		ImageReference reference = ImageReference.of("index.docker.io/ubuntu");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isNull();
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("docker.io/library/ubuntu");
	}

	@Test
	void ofNameAndTag() {
		ImageReference reference = ImageReference.of("ubuntu:bionic");
		assertThat(reference.getDomain()).isEqualTo("docker.io");
		assertThat(reference.getName()).isEqualTo("library/ubuntu");
		assertThat(reference.getTag()).isEqualTo("bionic");
		assertThat(reference.getDigest()).isNull();
		assertThat(reference.toString()).isEqualTo("docker.io/library/ubuntu:bionic");
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
		assertThat(reference.toString()).isEqualTo(
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
		assertThat(reference.toString()).isEqualTo(
				"docker.io/library/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
	}

	@Test
	void ofCustomDomainAndPortWithTag() {
		ImageReference reference = ImageReference.of(
				"example.com:8080/canonical/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference.getDomain()).isEqualTo("example.com:8080");
		assertThat(reference.getName()).isEqualTo("canonical/ubuntu");
		assertThat(reference.getTag()).isEqualTo("bionic");
		assertThat(reference.getDigest())
				.isEqualTo("sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference.toString()).isEqualTo(
				"example.com:8080/canonical/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
	}

	@Test
	void ofImageName() {
		ImageReference reference = ImageReference.of(ImageName.of("ubuntu"));
		assertThat(reference.toString()).isEqualTo("docker.io/library/ubuntu");
	}

	@Test
	void ofImageNameAndTag() {
		ImageReference reference = ImageReference.of(ImageName.of("ubuntu"), "bionic");
		assertThat(reference.toString()).isEqualTo("docker.io/library/ubuntu:bionic");
	}

	@Test
	void ofImageNameTagAndDigest() {
		ImageReference reference = ImageReference.of(ImageName.of("ubuntu"), "bionic",
				"sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThat(reference.toString()).isEqualTo(
				"docker.io/library/ubuntu:bionic@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
	}

	@Test
	void forJarFile() {
		assertForJarFile("spring-boot.2.0.0.BUILD-SNAPSHOT.jar", "library/spring-boot", "2.0.0.BUILD-SNAPSHOT");
		assertForJarFile("spring-boot.2.0.0.M1.jar", "library/spring-boot", "2.0.0.M1");
		assertForJarFile("spring-boot.2.0.0.RC1.jar", "library/spring-boot", "2.0.0.RC1");
		assertForJarFile("spring-boot.2.0.0.RELEASE.jar", "library/spring-boot", "2.0.0.RELEASE");
		assertForJarFile("sample-0.0.1-SNAPSHOT.jar", "library/sample", "0.0.1-SNAPSHOT");
		assertForJarFile("sample-0.0.1.jar", "library/sample", "0.0.1");
	}

	private void assertForJarFile(String jarFile, String expectedName, String expectedTag) {
		ImageReference reference = ImageReference.forJarFile(new File(jarFile));
		assertThat(reference.getName()).isEqualTo(expectedName);
		assertThat(reference.getTag()).isEqualTo(expectedTag);
	}

	@Test
	void randomGeneratesRandomName() {
		String prefix = "pack.local/builder/";
		ImageReference random = ImageReference.random(prefix);
		assertThat(random.toString()).startsWith(prefix).hasSize(prefix.length() + 10);
		ImageReference another = ImageReference.random(prefix);
		int attempts = 0;
		while (another.equals(random)) {
			assertThat(attempts).as("Duplicate results").isLessThan(10);
			another = ImageReference.random(prefix);
			attempts++;
		}
	}

	@Test
	void randomWithLengthGeneratesRandomName() {
		String prefix = "pack.local/builder/";
		ImageReference random = ImageReference.random(prefix, 20);
		assertThat(random.toString()).startsWith(prefix).hasSize(prefix.length() + 20);
	}

	@Test
	void randomWherePrefixIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageReference.random(null))
				.withMessage("Prefix must not be null");
	}

	@Test
	void inTaggedFormWhenHasDigestThrowsException() {
		ImageReference reference = ImageReference
				.of("ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
		assertThatIllegalStateException().isThrownBy(() -> reference.inTaggedForm()).withMessage(
				"Image reference 'docker.io/library/ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d' cannot contain a digest");
	}

	@Test
	void inTaggedFormWhenHasNoTagUsesLatest() {
		ImageReference reference = ImageReference.of("ubuntu");
		assertThat(reference.inTaggedForm().toString()).isEqualTo("docker.io/library/ubuntu:latest");
	}

	@Test
	void inTaggedFormWhenHasTagUsesTag() {
		ImageReference reference = ImageReference.of("ubuntu:bionic");
		assertThat(reference.inTaggedForm().toString()).isEqualTo("docker.io/library/ubuntu:bionic");
	}

	@Test
	void equalsAndHashCode() {
		ImageReference r1 = ImageReference.of("ubuntu:bionic");
		ImageReference r2 = ImageReference.of("docker.io/library/ubuntu:bionic");
		ImageReference r3 = ImageReference.of("docker.io/library/ubuntu:latest");
		assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
		assertThat(r1).isEqualTo(r1).isEqualTo(r2).isNotEqualTo(r3);
	}

}
