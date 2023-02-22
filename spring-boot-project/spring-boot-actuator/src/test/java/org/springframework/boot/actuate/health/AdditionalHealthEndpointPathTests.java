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

package org.springframework.boot.actuate.health;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AdditionalHealthEndpointPath}.
 *
 * @author Madhura Bhave
 */
class AdditionalHealthEndpointPathTests {

	@Test
	void fromValidPathShouldCreatePath() {
		AdditionalHealthEndpointPath path = AdditionalHealthEndpointPath.from("server:/my-path");
		assertThat(path.getValue()).isEqualTo("/my-path");
		assertThat(path.getNamespace()).isEqualTo(WebServerNamespace.SERVER);
	}

	@Test
	void fromValidPathWithoutSlashShouldCreatePath() {
		AdditionalHealthEndpointPath path = AdditionalHealthEndpointPath.from("server:my-path");
		assertThat(path.getValue()).isEqualTo("my-path");
		assertThat(path.getNamespace()).isEqualTo(WebServerNamespace.SERVER);
	}

	@Test
	void fromNullPathShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AdditionalHealthEndpointPath.from(null));
	}

	@Test
	void fromEmptyPathShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AdditionalHealthEndpointPath.from(""));
	}

	@Test
	void fromPathWithNoNamespaceShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AdditionalHealthEndpointPath.from("my-path"));
	}

	@Test
	void fromPathWithEmptyNamespaceShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AdditionalHealthEndpointPath.from(":my-path"));
	}

	@Test
	void fromPathWithMultipleSegmentsShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AdditionalHealthEndpointPath.from("server:/my-path/my-sub-path"));
	}

	@Test
	void fromPathWithMultipleSegmentsNotStartingWithSlashShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AdditionalHealthEndpointPath.from("server:my-path/my-sub-path"));
	}

	@Test
	void pathsWithTheSameNamespaceAndValueAreEqual() {
		assertThat(AdditionalHealthEndpointPath.from("server:/my-path"))
			.isEqualTo(AdditionalHealthEndpointPath.from("server:/my-path"));
	}

	@Test
	void pathsWithTheDifferentNamespaceAndSameValueAreNotEqual() {
		assertThat(AdditionalHealthEndpointPath.from("server:/my-path"))
			.isNotEqualTo((AdditionalHealthEndpointPath.from("management:/my-path")));
	}

	@Test
	void pathsWithTheSameNamespaceAndValuesWithNoSlashAreEqual() {
		assertThat(AdditionalHealthEndpointPath.from("server:/my-path"))
			.isEqualTo((AdditionalHealthEndpointPath.from("server:my-path")));
	}

	@Test
	void ofWithNullNamespaceShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> AdditionalHealthEndpointPath.of(null, "my-sub-path"));
	}

	@Test
	void ofWithNullPathShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AdditionalHealthEndpointPath.of(WebServerNamespace.SERVER, null));
	}

	@Test
	void ofWithMultipleSegmentValueShouldThrowException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> AdditionalHealthEndpointPath.of(WebServerNamespace.SERVER, "/my-path/my-subpath"));
	}

	@Test
	void ofShouldCreatePath() {
		AdditionalHealthEndpointPath additionalPath = AdditionalHealthEndpointPath.of(WebServerNamespace.SERVER,
				"my-path");
		assertThat(additionalPath.getValue()).isEqualTo("my-path");
		assertThat(additionalPath.getNamespace()).isEqualTo(WebServerNamespace.SERVER);
	}

}
