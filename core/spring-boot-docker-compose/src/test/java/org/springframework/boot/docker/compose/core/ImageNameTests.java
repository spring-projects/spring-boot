/*
 * Copyright 2012-present the original author or authors.
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
 * Tests for {@link ImageName}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ImageNameTests {

	@Test
	void ofWhenNameOnlyCreatesImageName() {
		ImageName imageName = ImageName.of("ubuntu");
		assertThat(imageName).hasToString("docker.io/library/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("library/ubuntu");
	}

	@Test
	void ofWhenSlashedNameCreatesImageName() {
		ImageName imageName = ImageName.of("canonical/ubuntu");
		assertThat(imageName).hasToString("docker.io/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenLocalhostNameCreatesImageName() {
		ImageName imageName = ImageName.of("localhost/canonical/ubuntu");
		assertThat(imageName).hasToString("localhost/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("localhost");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenDomainAndNameCreatesImageName() {
		ImageName imageName = ImageName.of("repo.spring.io/canonical/ubuntu");
		assertThat(imageName).hasToString("repo.spring.io/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo.spring.io");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenDomainNameAndPortCreatesImageName() {
		ImageName imageName = ImageName.of("repo.spring.io:8080/canonical/ubuntu");
		assertThat(imageName).hasToString("repo.spring.io:8080/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo.spring.io:8080");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenSimpleNameAndPortCreatesImageName() {
		ImageName imageName = ImageName.of("repo:8080/ubuntu");
		assertThat(imageName).hasToString("repo:8080/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo:8080");
		assertThat(imageName.getName()).isEqualTo("ubuntu");
	}

	@Test
	void ofWhenSimplePathAndPortCreatesImageName() {
		ImageName imageName = ImageName.of("repo:8080/canonical/ubuntu");
		assertThat(imageName).hasToString("repo:8080/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo:8080");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenNameWithLongPathCreatesImageName() {
		ImageName imageName = ImageName.of("path1/path2/path3/ubuntu");
		assertThat(imageName).hasToString("docker.io/path1/path2/path3/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("path1/path2/path3/ubuntu");
	}

	@Test
	void ofWhenLocalhostDomainCreatesImageName() {
		ImageName imageName = ImageName.of("localhost/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("localhost");
		assertThat(imageName.getName()).isEqualTo("ubuntu");
	}

	@Test
	void ofWhenLocalhostDomainAndPathCreatesImageName() {
		ImageName imageName = ImageName.of("localhost/library/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("localhost");
		assertThat(imageName.getName()).isEqualTo("library/ubuntu");
	}

	@Test
	void ofWhenLegacyDomainUsesNewDomain() {
		ImageName imageName = ImageName.of("index.docker.io/ubuntu");
		assertThat(imageName).hasToString("docker.io/library/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("library/ubuntu");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void ofWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of(null))
			.withMessage("'value' must not be empty");
	}

	@Test
	void ofWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of(""))
			.withMessage("'value' must not be empty");
	}

	@Test
	void ofWhenContainsUppercaseThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of("Test"))
			.withMessageContaining("must contain an image reference")
			.withMessageContaining("Test");
	}

	@Test
	void ofWhenNameIncludesTagThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of("ubuntu:latest"))
			.withMessageContaining("must contain an image reference")
			.withMessageContaining(":latest");
	}

	@Test
	void ofWhenNameIncludeDigestThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> ImageName.of("ubuntu@sha256:47bfdb88c3ae13e488167607973b7688f69d9e8c142c2045af343ec199649c09"))
			.withMessageContaining("must contain an image reference")
			.withMessageContaining("@sha256:47b");
	}

	@Test
	void hashCodeAndEquals() {
		ImageName n1 = ImageName.of("ubuntu");
		ImageName n2 = ImageName.of("library/ubuntu");
		ImageName n3 = ImageName.of("docker.io/ubuntu");
		ImageName n4 = ImageName.of("docker.io/library/ubuntu");
		ImageName n5 = ImageName.of("index.docker.io/library/ubuntu");
		ImageName n6 = ImageName.of("alpine");
		assertThat(n1).hasSameHashCodeAs(n2).hasSameHashCodeAs(n3).hasSameHashCodeAs(n4).hasSameHashCodeAs(n5);
		assertThat(n1).isEqualTo(n1).isEqualTo(n2).isEqualTo(n3).isEqualTo(n4).isNotEqualTo(n6);
	}

}
