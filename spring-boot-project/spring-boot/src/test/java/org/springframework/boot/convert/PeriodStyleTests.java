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

package org.springframework.boot.convert;

import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PeriodStyle}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 */
class PeriodStyleTests {

	@Test
	void detectAndParseWhenValueIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.detectAndParse(null))
				.withMessageContaining("Value must not be null");
	}

	@Test
	void detectAndParseWhenIso8601ShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("P15M")).isEqualTo(Period.parse("P15M"));
		assertThat(PeriodStyle.detectAndParse("-P15M")).isEqualTo(Period.parse("P-15M"));
		assertThat(PeriodStyle.detectAndParse("+P15M")).isEqualTo(Period.parse("P15M"));
		assertThat(PeriodStyle.detectAndParse("P2D")).isEqualTo(Period.parse("P2D"));
		assertThat(PeriodStyle.detectAndParse("-P20Y")).isEqualTo(Period.parse("P-20Y"));

	}

	@Test
	void detectAndParseWhenSimpleDaysShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("10d")).isEqualTo(Period.ofDays(10));
		assertThat(PeriodStyle.detectAndParse("10D")).isEqualTo(Period.ofDays(10));
		assertThat(PeriodStyle.detectAndParse("+10d")).isEqualTo(Period.ofDays(10));
		assertThat(PeriodStyle.detectAndParse("-10D")).isEqualTo(Period.ofDays(-10));
	}

	@Test
	void detectAndParseWhenSimpleWeeksShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("10w")).isEqualTo(Period.ofWeeks(10));
		assertThat(PeriodStyle.detectAndParse("10W")).isEqualTo(Period.ofWeeks(10));
		assertThat(PeriodStyle.detectAndParse("+10w")).isEqualTo(Period.ofWeeks(10));
		assertThat(PeriodStyle.detectAndParse("-10W")).isEqualTo(Period.ofWeeks(-10));
	}

	@Test
	void detectAndParseWhenSimpleMonthsShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("10m")).isEqualTo(Period.ofMonths(10));
		assertThat(PeriodStyle.detectAndParse("10M")).isEqualTo(Period.ofMonths(10));
		assertThat(PeriodStyle.detectAndParse("+10m")).isEqualTo(Period.ofMonths(10));
		assertThat(PeriodStyle.detectAndParse("-10M")).isEqualTo(Period.ofMonths(-10));
	}

	@Test
	void detectAndParseWhenSimpleYearsShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("10y")).isEqualTo(Period.ofYears(10));
		assertThat(PeriodStyle.detectAndParse("10Y")).isEqualTo(Period.ofYears(10));
		assertThat(PeriodStyle.detectAndParse("+10y")).isEqualTo(Period.ofYears(10));
		assertThat(PeriodStyle.detectAndParse("-10Y")).isEqualTo(Period.ofYears(-10));
	}

	@Test
	void detectAndParseWhenSimpleWithoutSuffixShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("10")).isEqualTo(Period.ofDays(10));
		assertThat(PeriodStyle.detectAndParse("+10")).isEqualTo(Period.ofDays(10));
		assertThat(PeriodStyle.detectAndParse("-10")).isEqualTo(Period.ofDays(-10));
	}

	@Test
	void detectAndParseWhenSimpleWithoutSuffixButWithChronoUnitShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("10", ChronoUnit.MONTHS)).isEqualTo(Period.ofMonths(10));
		assertThat(PeriodStyle.detectAndParse("+10", ChronoUnit.MONTHS)).isEqualTo(Period.ofMonths(10));
		assertThat(PeriodStyle.detectAndParse("-10", ChronoUnit.MONTHS)).isEqualTo(Period.ofMonths(-10));
	}

	@Test
	void detectAndParseWhenComplexShouldReturnPeriod() {
		assertThat(PeriodStyle.detectAndParse("1y2m")).isEqualTo(Period.of(1, 2, 0));
		assertThat(PeriodStyle.detectAndParse("1y2m3d")).isEqualTo(Period.of(1, 2, 3));
		assertThat(PeriodStyle.detectAndParse("2m3d")).isEqualTo(Period.of(0, 2, 3));
		assertThat(PeriodStyle.detectAndParse("1y3d")).isEqualTo(Period.of(1, 0, 3));
		assertThat(PeriodStyle.detectAndParse("-1y3d")).isEqualTo(Period.of(-1, 0, 3));
		assertThat(PeriodStyle.detectAndParse("-1y-3d")).isEqualTo(Period.of(-1, 0, -3));
	}

	@Test
	void detectAndParseWhenBadFormatShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.detectAndParse("10foo"))
				.withMessageContaining("'10foo' is not a valid period");
	}

	@Test
	void detectWhenSimpleShouldReturnSimple() {
		assertThat(PeriodStyle.detect("10")).isEqualTo(PeriodStyle.SIMPLE);
		assertThat(PeriodStyle.detect("+10")).isEqualTo(PeriodStyle.SIMPLE);
		assertThat(PeriodStyle.detect("-10")).isEqualTo(PeriodStyle.SIMPLE);
		assertThat(PeriodStyle.detect("10m")).isEqualTo(PeriodStyle.SIMPLE);
		assertThat(PeriodStyle.detect("10y")).isEqualTo(PeriodStyle.SIMPLE);
		assertThat(PeriodStyle.detect("10d")).isEqualTo(PeriodStyle.SIMPLE);
		assertThat(PeriodStyle.detect("10D")).isEqualTo(PeriodStyle.SIMPLE);
	}

	@Test
	void detectWhenIso8601ShouldReturnIso8601() {
		assertThat(PeriodStyle.detect("P20")).isEqualTo(PeriodStyle.ISO8601);
		assertThat(PeriodStyle.detect("-P15M")).isEqualTo(PeriodStyle.ISO8601);
		assertThat(PeriodStyle.detect("+P15M")).isEqualTo(PeriodStyle.ISO8601);
		assertThat(PeriodStyle.detect("P10Y")).isEqualTo(PeriodStyle.ISO8601);
		assertThat(PeriodStyle.detect("P2D")).isEqualTo(PeriodStyle.ISO8601);
		assertThat(PeriodStyle.detect("-P6")).isEqualTo(PeriodStyle.ISO8601);
		assertThat(PeriodStyle.detect("-P-6M")).isEqualTo(PeriodStyle.ISO8601);
	}

	@Test
	void detectWhenUnknownShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.detect("bad"))
				.withMessageContaining("'bad' is not a valid period");
	}

	@Test
	void parseIso8601ShouldParse() {
		assertThat(PeriodStyle.ISO8601.parse("P20D")).isEqualTo(Period.parse("P20D"));
		assertThat(PeriodStyle.ISO8601.parse("P15M")).isEqualTo(Period.parse("P15M"));
		assertThat(PeriodStyle.ISO8601.parse("+P15M")).isEqualTo(Period.parse("P15M"));
		assertThat(PeriodStyle.ISO8601.parse("P10Y")).isEqualTo(Period.parse("P10Y"));
		assertThat(PeriodStyle.ISO8601.parse("P2D")).isEqualTo(Period.parse("P2D"));
		assertThat(PeriodStyle.ISO8601.parse("-P6D")).isEqualTo(Period.parse("-P6D"));
		assertThat(PeriodStyle.ISO8601.parse("-P-6Y+3M")).isEqualTo(Period.parse("-P-6Y+3M"));
	}

	@Test
	void parseIso8601WithUnitShouldIgnoreUnit() {
		assertThat(PeriodStyle.ISO8601.parse("P20D", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P20D"));
		assertThat(PeriodStyle.ISO8601.parse("P15M", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P15M"));
		assertThat(PeriodStyle.ISO8601.parse("+P15M", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P15M"));
		assertThat(PeriodStyle.ISO8601.parse("P10Y", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P10Y"));
		assertThat(PeriodStyle.ISO8601.parse("P2D", ChronoUnit.SECONDS)).isEqualTo(Period.parse("P2D"));
		assertThat(PeriodStyle.ISO8601.parse("-P6D", ChronoUnit.SECONDS)).isEqualTo(Period.parse("-P6D"));
		assertThat(PeriodStyle.ISO8601.parse("-P-6Y+3M", ChronoUnit.SECONDS)).isEqualTo(Period.parse("-P-6Y+3M"));
	}

	@Test
	void parseIso8601WhenSimpleShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.ISO8601.parse("10d"))
				.withMessageContaining("'10d' is not a valid ISO-8601 period");
	}

	@Test
	void parseSimpleShouldParse() {
		assertThat(PeriodStyle.SIMPLE.parse("10m")).isEqualTo(Period.ofMonths(10));
	}

	@Test
	void parseSimpleWithUnitShouldUseUnitAsFallback() {
		assertThat(PeriodStyle.SIMPLE.parse("10m", ChronoUnit.DAYS)).isEqualTo(Period.ofMonths(10));
		assertThat(PeriodStyle.SIMPLE.parse("10", ChronoUnit.MONTHS)).isEqualTo(Period.ofMonths(10));
	}

	@Test
	void parseSimpleWhenUnknownUnitShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.SIMPLE.parse("10x")).satisfies(
				(ex) -> assertThat(ex.getCause().getMessage()).isEqualTo("Does not match simple period pattern"));
	}

	@Test
	void parseSimpleWhenIso8601ShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> PeriodStyle.SIMPLE.parse("PT10H"))
				.withMessageContaining("'PT10H' is not a valid simple period");
	}

	@Test
	void printIso8601ShouldPrint() {
		Period period = Period.parse("-P-6M+3D");
		assertThat(PeriodStyle.ISO8601.print(period)).isEqualTo("P6M-3D");
	}

	@Test
	void printIso8601ShouldIgnoreUnit() {
		Period period = Period.parse("-P3Y");
		assertThat(PeriodStyle.ISO8601.print(period, ChronoUnit.DAYS)).isEqualTo("P-3Y");
	}

	@Test
	void printSimpleWhenZeroWithoutUnitShouldPrintInDays() {
		Period period = Period.ofMonths(0);
		assertThat(PeriodStyle.SIMPLE.print(period)).isEqualTo("0d");
	}

	@Test
	void printSimpleWhenZeroWithUnitShouldPrintInUnit() {
		Period period = Period.ofYears(0);
		assertThat(PeriodStyle.SIMPLE.print(period, ChronoUnit.YEARS)).isEqualTo("0y");
	}

	@Test
	void printSimpleWhenNonZeroShouldIgnoreUnit() {
		Period period = Period.of(1, 2, 3);
		assertThat(PeriodStyle.SIMPLE.print(period, ChronoUnit.YEARS)).isEqualTo("1y2m3d");
	}

}
