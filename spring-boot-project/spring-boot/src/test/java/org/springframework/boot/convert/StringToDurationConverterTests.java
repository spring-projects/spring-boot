/*
 * Copyright 2012-2022 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link StringToDurationConverter}.
 *
 * @author Phillip Webb
 */
class StringToDurationConverterTests {

	@ConversionServiceTest
	void convertWhenIso8601ShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "PT20.345S")).isEqualTo(Duration.parse("PT20.345S"));
		assertThat(convert(conversionService, "PT15M")).isEqualTo(Duration.parse("PT15M"));
		assertThat(convert(conversionService, "+PT15M")).isEqualTo(Duration.parse("PT15M"));
		assertThat(convert(conversionService, "PT10H")).isEqualTo(Duration.parse("PT10H"));
		assertThat(convert(conversionService, "P2D")).isEqualTo(Duration.parse("P2D"));
		assertThat(convert(conversionService, "P2DT3H4M")).isEqualTo(Duration.parse("P2DT3H4M"));
		assertThat(convert(conversionService, "-PT6H3M")).isEqualTo(Duration.parse("-PT6H3M"));
		assertThat(convert(conversionService, "-PT-6H+3M")).isEqualTo(Duration.parse("-PT-6H+3M"));
	}

	@ConversionServiceTest
	void convertWhenSimpleNanosShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10ns")).hasNanos(10);
		assertThat(convert(conversionService, "10NS")).hasNanos(10);
		assertThat(convert(conversionService, "+10ns")).hasNanos(10);
		assertThat(convert(conversionService, "-10ns")).hasNanos(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleMicrosShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10us")).hasNanos(10000);
		assertThat(convert(conversionService, "10US")).hasNanos(10000);
		assertThat(convert(conversionService, "+10us")).hasNanos(10000);
		assertThat(convert(conversionService, "-10us")).hasNanos(-10000);
	}

	@ConversionServiceTest
	void convertWhenSimpleMillisShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10ms")).hasMillis(10);
		assertThat(convert(conversionService, "10MS")).hasMillis(10);
		assertThat(convert(conversionService, "+10ms")).hasMillis(10);
		assertThat(convert(conversionService, "-10ms")).hasMillis(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleSecondsShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10s")).hasSeconds(10);
		assertThat(convert(conversionService, "10S")).hasSeconds(10);
		assertThat(convert(conversionService, "+10s")).hasSeconds(10);
		assertThat(convert(conversionService, "-10s")).hasSeconds(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleMinutesShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10m")).hasMinutes(10);
		assertThat(convert(conversionService, "10M")).hasMinutes(10);
		assertThat(convert(conversionService, "+10m")).hasMinutes(10);
		assertThat(convert(conversionService, "-10m")).hasMinutes(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleHoursShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10h")).hasHours(10);
		assertThat(convert(conversionService, "10H")).hasHours(10);
		assertThat(convert(conversionService, "+10h")).hasHours(10);
		assertThat(convert(conversionService, "-10h")).hasHours(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleDaysShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10d")).hasDays(10);
		assertThat(convert(conversionService, "10D")).hasDays(10);
		assertThat(convert(conversionService, "+10d")).hasDays(10);
		assertThat(convert(conversionService, "-10d")).hasDays(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10")).hasMillis(10);
		assertThat(convert(conversionService, "+10")).hasMillis(10);
		assertThat(convert(conversionService, "-10")).hasMillis(-10);
	}

	@ConversionServiceTest
	void convertWhenSimpleWithoutSuffixButWithAnnotationShouldReturnDuration(ConversionService conversionService) {
		assertThat(convert(conversionService, "10", ChronoUnit.SECONDS, null)).hasSeconds(10);
		assertThat(convert(conversionService, "+10", ChronoUnit.SECONDS, null)).hasSeconds(10);
		assertThat(convert(conversionService, "-10", ChronoUnit.SECONDS, null)).hasSeconds(-10);
	}

	@ConversionServiceTest
	void convertWhenBadFormatShouldThrowException(ConversionService conversionService) {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() -> convert(conversionService, "10foo"))
				.havingRootCause().withMessageContaining("'10foo' is not a valid duration");
	}

	@ConversionServiceTest
	void convertWhenStyleMismatchShouldThrowException(ConversionService conversionService) {
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> convert(conversionService, "10s", null, DurationStyle.ISO8601));
	}

	@ConversionServiceTest
	void convertWhenEmptyShouldReturnNull(ConversionService conversionService) {
		assertThat(convert(conversionService, "")).isNull();
	}

	private Duration convert(ConversionService conversionService, String source) {
		return conversionService.convert(source, Duration.class);
	}

	private Duration convert(ConversionService conversionService, String source, ChronoUnit unit, DurationStyle style) {
		return (Duration) conversionService.convert(source, TypeDescriptor.forObject(source),
				MockDurationTypeDescriptor.get(unit, style));
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new StringToDurationConverter());
	}

}
