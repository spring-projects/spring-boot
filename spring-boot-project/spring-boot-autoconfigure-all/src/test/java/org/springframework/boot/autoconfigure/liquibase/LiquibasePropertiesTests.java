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

package org.springframework.boot.autoconfigure.liquibase;

import java.util.List;
import java.util.stream.Stream;

import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.ui.UIServiceEnum;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties.ShowSummary;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties.ShowSummaryOutput;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties.UiService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseProperties}.
 *
 * @author Andy Wilkinson
 */
class LiquibasePropertiesTests {

	@Test
	void valuesOfShowSummaryMatchValuesOfUpdateSummaryEnum() {
		assertThat(namesOf(ShowSummary.values())).isEqualTo(namesOf(UpdateSummaryEnum.values()));
	}

	@Test
	void valuesOfShowSummaryOutputMatchValuesOfUpdateSummaryOutputEnum() {
		assertThat(namesOf(ShowSummaryOutput.values())).isEqualTo(namesOf(UpdateSummaryOutputEnum.values()));
	}

	@Test
	void valuesOfUiServiceMatchValuesOfUiServiceEnum() {
		assertThat(namesOf(UiService.values())).isEqualTo(namesOf(UIServiceEnum.values()));
	}

	private List<String> namesOf(Enum<?>[] input) {
		return Stream.of(input).map(Enum::name).toList();
	}

}
