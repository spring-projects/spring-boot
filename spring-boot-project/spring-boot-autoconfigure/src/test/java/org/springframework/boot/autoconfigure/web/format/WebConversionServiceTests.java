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

package org.springframework.boot.autoconfigure.web.format;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebConversionService}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Gaurav Pareek
 */
class WebConversionServiceTests {

	@Test
	void defaultDateFormat() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters());
		LocalDate date = LocalDate.of(2020, 4, 26);
		assertThat(conversionService.convert(date, String.class))
			.isEqualTo(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(date));
	}

	@Test
	void isoDateFormat() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters().dateFormat("iso"));
		LocalDate date = LocalDate.of(2020, 4, 26);
		assertThat(conversionService.convert(date, String.class))
			.isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
	}

	@Test
	void customDateFormatWithJavaUtilDate() {
		customDateFormat(Date.from(ZonedDateTime.of(2018, 1, 1, 20, 30, 0, 0, ZoneId.systemDefault()).toInstant()));
	}

	@Test
	void customDateFormatWithJavaTime() {
		customDateFormat(java.time.LocalDate.of(2018, 1, 1));
	}

	@Test
	void defaultTimeFormat() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters());
		LocalTime time = LocalTime.of(12, 45, 23);
		assertThat(conversionService.convert(time, String.class))
			.isEqualTo(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time));
	}

	@Test
	void isoTimeFormat() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters().timeFormat("iso"));
		LocalTime time = LocalTime.of(12, 45, 23);
		assertThat(conversionService.convert(time, String.class))
			.isEqualTo(DateTimeFormatter.ISO_LOCAL_TIME.format(time));
	}

	@Test
	void isoOffsetTimeFormat() {
		isoOffsetTimeFormat(new DateTimeFormatters().timeFormat("isooffset"));
	}

	@Test
	void hyphenatedIsoOffsetTimeFormat() {
		isoOffsetTimeFormat(new DateTimeFormatters().timeFormat("iso-offset"));
	}

	private void isoOffsetTimeFormat(DateTimeFormatters formatters) {
		WebConversionService conversionService = new WebConversionService(formatters);
		OffsetTime offsetTime = OffsetTime.of(LocalTime.of(12, 45, 23), ZoneOffset.ofHoursMinutes(1, 30));
		assertThat(conversionService.convert(offsetTime, String.class))
			.isEqualTo(DateTimeFormatter.ISO_OFFSET_TIME.format(offsetTime));
	}

	@Test
	void customTimeFormat() {
		WebConversionService conversionService = new WebConversionService(
				new DateTimeFormatters().timeFormat("HH*mm*ss"));
		LocalTime time = LocalTime.of(12, 45, 23);
		assertThat(conversionService.convert(time, String.class)).isEqualTo("12*45*23");
	}

	@Test
	void defaultDateTimeFormat() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters());
		LocalDateTime dateTime = LocalDateTime.of(2020, 4, 26, 12, 45, 23);
		assertThat(conversionService.convert(dateTime, String.class))
			.isEqualTo(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(dateTime));
	}

	@Test
	void isoDateTimeFormat() {
		WebConversionService conversionService = new WebConversionService(
				new DateTimeFormatters().dateTimeFormat("iso"));
		LocalDateTime dateTime = LocalDateTime.of(2020, 4, 26, 12, 45, 23);
		assertThat(conversionService.convert(dateTime, String.class))
			.isEqualTo(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime));
	}

	@Test
	void isoOffsetDateTimeFormat() {
		isoOffsetDateTimeFormat(new DateTimeFormatters().dateTimeFormat("isooffset"));
	}

	@Test
	void hyphenatedIsoOffsetDateTimeFormat() {
		isoOffsetDateTimeFormat(new DateTimeFormatters().dateTimeFormat("iso-offset"));
	}

	private void isoOffsetDateTimeFormat(DateTimeFormatters formatters) {
		WebConversionService conversionService = new WebConversionService(formatters);
		OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDate.of(2020, 4, 26), LocalTime.of(12, 45, 23),
				ZoneOffset.ofHoursMinutes(1, 30));
		assertThat(conversionService.convert(offsetDateTime, String.class))
			.isEqualTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime));
	}

	@Test
	void customDateTimeFormat() {
		WebConversionService conversionService = new WebConversionService(
				new DateTimeFormatters().dateTimeFormat("dd*MM*yyyy HH*mm*ss"));
		LocalDateTime dateTime = LocalDateTime.of(2020, 4, 26, 12, 45, 23);
		assertThat(conversionService.convert(dateTime, String.class)).isEqualTo("26*04*2020 12*45*23");
	}

	@Test
	void convertFromStringToLocalDate() {
		WebConversionService conversionService = new WebConversionService(
				new DateTimeFormatters().dateFormat("yyyy-MM-dd"));
		LocalDate date = conversionService.convert("2018-01-01", LocalDate.class);
		assertThat(date).isEqualTo(java.time.LocalDate.of(2018, 1, 1));
	}

	@Test
	void convertFromStringToLocalDateWithIsoFormatting() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters().dateFormat("iso"));
		LocalDate date = conversionService.convert("2018-01-01", LocalDate.class);
		assertThat(date).isEqualTo(java.time.LocalDate.of(2018, 1, 1));
	}

	@Test
	void convertFromStringToDateWithIsoFormatting() {
		WebConversionService conversionService = new WebConversionService(new DateTimeFormatters().dateFormat("iso"));
		Date date = conversionService.convert("2018-01-01", Date.class);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		assertThat(calendar.get(Calendar.YEAR)).isEqualTo(2018);
		assertThat(calendar.get(Calendar.MONTH)).isZero();
		assertThat(calendar.get(Calendar.DAY_OF_MONTH)).isOne();
	}

	private void customDateFormat(Object input) {
		WebConversionService conversionService = new WebConversionService(
				new DateTimeFormatters().dateFormat("dd*MM*yyyy"));
		assertThat(conversionService.convert(input, String.class)).isEqualTo("01*01*2018");
	}

}
