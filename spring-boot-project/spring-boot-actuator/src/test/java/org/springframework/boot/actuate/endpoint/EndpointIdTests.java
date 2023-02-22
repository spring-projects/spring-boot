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

package org.springframework.boot.actuate.endpoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link EndpointId}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
class EndpointIdTests {

	@Test
	void ofWhenNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of(null))
			.withMessage("Value must not be empty");
	}

	@Test
	void ofWhenEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of("")).withMessage("Value must not be empty");
	}

	@Test
	void ofWhenContainsSlashThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of("foo/bar"))
			.withMessage("Value must only contain valid chars");
	}

	@Test
	void ofWhenContainsBackslashThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of("foo\\bar"))
			.withMessage("Value must only contain valid chars");
	}

	@Test
	void ofWhenHasBadCharThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of("foo!bar"))
			.withMessage("Value must only contain valid chars");
	}

	@Test
	void ofWhenStartsWithNumberThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of("1foo"))
			.withMessage("Value must not start with a number");
	}

	@Test
	void ofWhenStartsWithUppercaseLetterThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> EndpointId.of("Foo"))
			.withMessage("Value must not start with an uppercase letter");
	}

	@Test
	void ofWhenContainsDotIsValid() {
		// Ideally we wouldn't support this but there are existing endpoints using the
		// pattern. See gh-14773
		EndpointId endpointId = EndpointId.of("foo.bar");
		assertThat(endpointId).hasToString("foo.bar");
	}

	@Test
	void ofWhenContainsDashIsValid() {
		// Ideally we wouldn't support this but there are existing endpoints using the
		// pattern. See gh-14773
		EndpointId endpointId = EndpointId.of("foo-bar");
		assertThat(endpointId).hasToString("foo-bar");
	}

	@Test
	void ofWhenContainsDeprecatedCharsLogsWarning(CapturedOutput output) {
		EndpointId.resetLoggedWarnings();
		EndpointId.of("foo-bar");
		assertThat(output)
			.contains("Endpoint ID 'foo-bar' contains invalid characters, please migrate to a valid format");
	}

	@Test
	void ofWhenMigratingLegacyNameRemovesDots(CapturedOutput output) {
		EndpointId endpointId = migrateLegacyName("one.two.three");
		assertThat(endpointId).hasToString("onetwothree");
		assertThat(output).doesNotContain("contains invalid characters");
	}

	@Test
	void ofWhenMigratingLegacyNameRemovesHyphens(CapturedOutput output) {
		EndpointId endpointId = migrateLegacyName("one-two-three");
		assertThat(endpointId).hasToString("onetwothree");
		assertThat(output).doesNotContain("contains invalid characters");
	}

	@Test
	void ofWhenMigratingLegacyNameRemovesMixOfDashAndDot(CapturedOutput output) {
		EndpointId endpointId = migrateLegacyName("one.two-three");
		assertThat(endpointId).hasToString("onetwothree");
		assertThat(output).doesNotContain("contains invalid characters");
	}

	private EndpointId migrateLegacyName(String name) {
		EndpointId.resetLoggedWarnings();
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("management.endpoints.migrate-legacy-ids", "true");
		return EndpointId.of(environment, name);
	}

	@Test
	void equalsAndHashCode() {
		EndpointId one = EndpointId.of("foobar1");
		EndpointId two = EndpointId.of("fooBar1");
		EndpointId three = EndpointId.of("foo-bar1");
		EndpointId four = EndpointId.of("foo.bar1");
		EndpointId five = EndpointId.of("barfoo1");
		EndpointId six = EndpointId.of("foobar2");
		assertThat(one).hasSameHashCodeAs(two);
		assertThat(one).isEqualTo(one)
			.isEqualTo(two)
			.isEqualTo(three)
			.isEqualTo(four)
			.isNotEqualTo(five)
			.isNotEqualTo(six);
	}

	@Test
	void toLowerCaseStringReturnsLowercase() {
		assertThat(EndpointId.of("fooBar").toLowerCaseString()).isEqualTo("foobar");
	}

	@Test
	void toStringReturnsString() {
		assertThat(EndpointId.of("fooBar")).hasToString("fooBar");
	}

	@Test
	void fromPropertyValueStripsDashes() {
		EndpointId fromPropertyValue = EndpointId.fromPropertyValue("foo-bar");
		assertThat(fromPropertyValue).isEqualTo(EndpointId.of("fooBar"));
	}

}
