/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IsoOffsetFormatter}.
 *
 * @author Phillip Webb
 */
class IsoOffsetFormatterTests {

	@ConversionServiceTest
	void convertShouldConvertStringToIsoDate(ConversionService conversionService) {
		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime converted = conversionService.convert(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now),
				OffsetDateTime.class);
		assertThat(converted).isEqualTo(now);
	}

	@ConversionServiceTest
	void convertShouldConvertIsoDateToString(ConversionService conversionService) {
		OffsetDateTime now = OffsetDateTime.now();
		String converted = conversionService.convert(now, String.class);
		assertThat(converted).isNotNull().startsWith(now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}

	static Stream<? extends Arguments> conversionServices() {
		return ConversionServiceArguments.with(new IsoOffsetFormatter());
	}

}
