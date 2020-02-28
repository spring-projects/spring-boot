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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ImageName}.
 *
 * @author Phillip Webb
 */
class ImageNameTests {

	@Test
	void ofWhenNameOnlyCreatesImageName() {
		ImageName imageName = ImageName.of("ubuntu");
		assertThat(imageName.toString()).isEqualTo("docker.io/library/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("library/ubuntu");
	}

	@Test
	void ofWhenSlashedNameCreatesImageName() {
		ImageName imageName = ImageName.of("canonical/ubuntu");
		assertThat(imageName.toString()).isEqualTo("docker.io/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenLocalhostNameCreatesImageName() {
		ImageName imageName = ImageName.of("localhost/canonical/ubuntu");
		assertThat(imageName.toString()).isEqualTo("localhost/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("localhost");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenDomainAndNameCreatesImageName() {
		ImageName imageName = ImageName.of("repo.spring.io/canonical/ubuntu");
		assertThat(imageName.toString()).isEqualTo("repo.spring.io/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo.spring.io");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenDomainNameAndPortCreatesImageName() {
		ImageName imageName = ImageName.of("repo.spring.io:8080/canonical/ubuntu");
		assertThat(imageName.toString()).isEqualTo("repo.spring.io:8080/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo.spring.io:8080");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenSimpleNameAndPortCreatesImageName() {
		ImageName imageName = ImageName.of("repo:8080/canonical/ubuntu");
		assertThat(imageName.toString()).isEqualTo("repo:8080/canonical/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("repo:8080");
		assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
	}

	@Test
	void ofWhenLegacyDomainUsesNewDomain() {
		ImageName imageName = ImageName.of("index.docker.io/ubuntu");
		assertThat(imageName.toString()).isEqualTo("docker.io/library/ubuntu");
		assertThat(imageName.getDomain()).isEqualTo("docker.io");
		assertThat(imageName.getName()).isEqualTo("library/ubuntu");
	}

	@Test
	void ofWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of(null))
				.withMessage("Value must not be empty");
	}

	@Test
	void ofWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of("")).withMessage("Value must not be empty");
	}

	@Test
	void hashCodeAndEquals() {
		ImageName n1 = ImageName.of("ubuntu");
		ImageName n2 = ImageName.of("library/ubuntu");
		ImageName n3 = ImageName.of("docker.io/library/ubuntu");
		ImageName n4 = ImageName.of("index.docker.io/library/ubuntu");
		ImageName n5 = ImageName.of("alpine");
		assertThat(n1.hashCode()).isEqualTo(n2.hashCode()).isEqualTo(n3.hashCode()).isEqualTo(n4.hashCode());
		assertThat(n1).isEqualTo(n1).isEqualTo(n2).isEqualTo(n3).isEqualTo(n4).isNotEqualTo(n5);
	}

}
