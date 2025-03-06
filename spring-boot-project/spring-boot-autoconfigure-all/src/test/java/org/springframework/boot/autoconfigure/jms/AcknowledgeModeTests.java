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

package org.springframework.boot.autoconfigure.jms;

import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AcknowledgeMode}.
 *
 * @author Andy Wilkinson
 */
class AcknowledgeModeTests {

	@ParameterizedTest
	@EnumSource
	void stringIsMappedToInt(Mapping mapping) {
		assertThat(AcknowledgeMode.of(mapping.actual)).extracting(AcknowledgeMode::getMode).isEqualTo(mapping.expected);
	}

	@Test
	void mapShouldThrowWhenMapIsCalledWithUnknownNonIntegerString() {
		assertThatIllegalArgumentException().isThrownBy(() -> AcknowledgeMode.of("some-string"))
			.withMessage(
					"'some-string' is neither a known acknowledge mode (auto, client, or dups_ok) nor an integer value");
	}

	private enum Mapping {

		AUTO_LOWER_CASE("auto", Session.AUTO_ACKNOWLEDGE),

		CLIENT_LOWER_CASE("client", Session.CLIENT_ACKNOWLEDGE),

		DUPS_OK_LOWER_CASE("dups_ok", Session.DUPS_OK_ACKNOWLEDGE),

		AUTO_UPPER_CASE("AUTO", Session.AUTO_ACKNOWLEDGE),

		CLIENT_UPPER_CASE("CLIENT", Session.CLIENT_ACKNOWLEDGE),

		DUPS_OK_UPPER_CASE("DUPS_OK", Session.DUPS_OK_ACKNOWLEDGE),

		AUTO_MIXED_CASE("AuTo", Session.AUTO_ACKNOWLEDGE),

		CLIENT_MIXED_CASE("CliEnT", Session.CLIENT_ACKNOWLEDGE),

		DUPS_OK_MIXED_CASE("dUPs_Ok", Session.DUPS_OK_ACKNOWLEDGE),

		DUPS_OK_KEBAB_CASE("DUPS-OK", Session.DUPS_OK_ACKNOWLEDGE),

		DUPS_OK_NO_SEPARATOR_UPPER_CASE("DUPSOK", Session.DUPS_OK_ACKNOWLEDGE),

		DUPS_OK_NO_SEPARATOR_LOWER_CASE("dupsok", Session.DUPS_OK_ACKNOWLEDGE),

		DUPS_OK_NO_SEPARATOR_MIXED_CASE("duPSok", Session.DUPS_OK_ACKNOWLEDGE),

		INTEGER("36", 36);

		private final String actual;

		private final int expected;

		Mapping(String actual, int expected) {
			this.actual = actual;
			this.expected = expected;
		}

	}

}
