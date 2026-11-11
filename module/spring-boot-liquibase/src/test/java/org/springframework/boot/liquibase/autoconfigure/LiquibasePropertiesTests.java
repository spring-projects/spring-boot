/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.autoconfigure;

import java.util.List;
import java.util.stream.Stream;

import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.ui.UIServiceEnum;
import org.junit.jupiter.api.Test;

import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties.ShowSummary;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties.ShowSummaryOutput;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties.UiService;

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
