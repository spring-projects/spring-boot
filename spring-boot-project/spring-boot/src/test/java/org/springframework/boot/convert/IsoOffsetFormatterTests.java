/*
 * Copyright 2012-2019 the original author or authors.
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
