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

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ApiVersions}.
 *
 * @author Scott Frederick
 */
class ApiVersionsTests {

	@Test
	void findsLatestWhenOneMatchesMajor() {
		ApiVersion version = ApiVersions.parse("1.1", "2.2").findLatestSupported("1.0");
		assertThat(version).isEqualTo(ApiVersion.parse("1.1"));
	}

	@Test
	void findsLatestWhenOneMatchesWithReleaseVersions() {
		ApiVersion version = ApiVersions.parse("1.1", "1.2").findLatestSupported("1.1");
		assertThat(version).isEqualTo(ApiVersion.parse("1.2"));
	}

	@Test
	void findsLatestWhenOneMatchesWithPreReleaseVersions() {
		ApiVersion version = ApiVersions.parse("0.2", "0.3").findLatestSupported("0.2");
		assertThat(version).isEqualTo(ApiVersion.parse("0.2"));
	}

	@Test
	void findsLatestWhenMultipleMatchesWithReleaseVersions() {
		ApiVersion version = ApiVersions.parse("1.1", "1.2").findLatestSupported("1.1", "1.2");
		assertThat(version).isEqualTo(ApiVersion.parse("1.2"));
	}

	@Test
	void findsLatestWhenMultipleMatchesWithPreReleaseVersions() {
		ApiVersion version = ApiVersions.parse("0.2", "0.3").findLatestSupported("0.2", "0.3");
		assertThat(version).isEqualTo(ApiVersion.parse("0.3"));
	}

	@Test
	void findLatestWhenNoneSupportedThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> ApiVersions.parse("1.1", "1.2").findLatestSupported("1.3", "1.4")).withMessage(
						"Detected platform API versions '1.3,1.4' are not included in supported versions '1.1,1.2'");
	}

	@Test
	void toStringReturnsString() {
		assertThat(ApiVersions.parse("1.1", "2.2", "3.3").toString()).isEqualTo("1.1,2.2,3.3");
	}

	@Test
	void equalsAndHashCode() {
		ApiVersions v12a = ApiVersions.parse("1.2", "2.3");
		ApiVersions v12b = ApiVersions.parse("1.2", "2.3");
		ApiVersions v13 = ApiVersions.parse("1.3", "2.4");
		assertThat(v12a.hashCode()).isEqualTo(v12b.hashCode());
		assertThat(v12a).isEqualTo(v12a).isEqualTo(v12b).isNotEqualTo(v13);
	}

}
