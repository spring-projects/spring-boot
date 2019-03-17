/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.format;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebConversionService}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
public class WebConversionServiceTests {

	@Test
	public void customDateFormat() {
		WebConversionService conversionService = new WebConversionService("dd*MM*yyyy");
		Date date = new DateTime(2018, 1, 1, 20, 30).toDate();
		assertThat(conversionService.convert(date, String.class)).isEqualTo("01*01*2018");
		LocalDate jodaDate = LocalDate.fromDateFields(date);
		assertThat(conversionService.convert(jodaDate, String.class))
				.isEqualTo("01*01*2018");
		java.time.LocalDate localDate = java.time.LocalDate.of(2018, 1, 1);
		assertThat(conversionService.convert(localDate, String.class))
				.isEqualTo("01*01*2018");
	}

	@Test
	public void convertFromStringToDate() {
		WebConversionService conversionService = new WebConversionService("yyyy-MM-dd");
		java.time.LocalDate date = conversionService.convert("2018-01-01",
				java.time.LocalDate.class);
		assertThat(date).isEqualTo(java.time.LocalDate.of(2018, 1, 1));
	}

}
