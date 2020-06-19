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
 * Tests for {@link ImageReferenceParser}.
 *
 * @author Scott Frederick
 */
class ImageReferenceParserTests {

	@Test
	void unableToParseWithUppercaseInName() {
		assertThatIllegalArgumentException().isThrownBy(() -> ImageReferenceParser.of("Test"))
				.withMessageContaining("Test");
	}

	@Test
	void parsesName() {
		ImageReferenceParser parser = ImageReferenceParser.of("ubuntu");
		assertThat(parser.getDomain()).isNull();
		assertThat(parser.getName()).isEqualTo("ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesNameWithPath() {
		ImageReferenceParser parser = ImageReferenceParser.of("library/ubuntu");
		assertThat(parser.getDomain()).isNull();
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesNameWithLongPath() {
		ImageReferenceParser parser = ImageReferenceParser.of("path1/path2/path3/ubuntu");
		assertThat(parser.getDomain()).isNull();
		assertThat(parser.getName()).isEqualTo("path1/path2/path3/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesDomainAndNameWithPath() {
		ImageReferenceParser parser = ImageReferenceParser.of("repo.example.com/library/ubuntu");
		assertThat(parser.getDomain()).isEqualTo("repo.example.com");
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesDomainWithPortAndNameWithPath() {
		ImageReferenceParser parser = ImageReferenceParser.of("repo.example.com:8080/library/ubuntu");
		assertThat(parser.getDomain()).isEqualTo("repo.example.com:8080");
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesSimpleDomainWithPortAndName() {
		ImageReferenceParser parser = ImageReferenceParser.of("repo:8080/ubuntu");
		assertThat(parser.getDomain()).isEqualTo("repo:8080");
		assertThat(parser.getName()).isEqualTo("ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesSimpleDomainWithPortAndNameWithPath() {
		ImageReferenceParser parser = ImageReferenceParser.of("repo:8080/library/ubuntu");
		assertThat(parser.getDomain()).isEqualTo("repo:8080");
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesLocalhostDomainAndName() {
		ImageReferenceParser parser = ImageReferenceParser.of("localhost/ubuntu");
		assertThat(parser.getDomain()).isEqualTo("localhost");
		assertThat(parser.getName()).isEqualTo("ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesLocalhostDomainAndNameWithPath() {
		ImageReferenceParser parser = ImageReferenceParser.of("localhost/library/ubuntu");
		assertThat(parser.getDomain()).isEqualTo("localhost");
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesNameAndTag() {
		ImageReferenceParser parser = ImageReferenceParser.of("ubuntu:v1");
		assertThat(parser.getDomain()).isNull();
		assertThat(parser.getName()).isEqualTo("ubuntu");
		assertThat(parser.getTag()).isEqualTo("v1");
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesNameAndDigest() {
		ImageReferenceParser parser = ImageReferenceParser
				.of("ubuntu@sha256:47bfdb88c3ae13e488167607973b7688f69d9e8c142c2045af343ec199649c09");
		assertThat(parser.getDomain()).isNull();
		assertThat(parser.getName()).isEqualTo("ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest())
				.isEqualTo("sha256:47bfdb88c3ae13e488167607973b7688f69d9e8c142c2045af343ec199649c09");
	}

	@Test
	void parsesReferenceWithTag() {
		ImageReferenceParser parser = ImageReferenceParser.of("repo.example.com:8080/library/ubuntu:v1");
		assertThat(parser.getDomain()).isEqualTo("repo.example.com:8080");
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isEqualTo("v1");
		assertThat(parser.getDigest()).isNull();
	}

	@Test
	void parsesReferenceWithDigest() {
		ImageReferenceParser parser = ImageReferenceParser.of(
				"repo.example.com:8080/library/ubuntu@sha256:47bfdb88c3ae13e488167607973b7688f69d9e8c142c2045af343ec199649c09");
		assertThat(parser.getDomain()).isEqualTo("repo.example.com:8080");
		assertThat(parser.getName()).isEqualTo("library/ubuntu");
		assertThat(parser.getTag()).isNull();
		assertThat(parser.getDigest())
				.isEqualTo("sha256:47bfdb88c3ae13e488167607973b7688f69d9e8c142c2045af343ec199649c09");
	}

}
