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

package org.springframework.boot.convert;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.core.convert.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IsoOffsetFormatter}.
 *
 * @author Phillip Webb
 */
@RunWith(Parameterized.class)
public class IsoOffsetFormatterTests {

	private final ConversionService conversionService;

	public IsoOffsetFormatterTests(String name, ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Test
	public void convertShouldConvertStringToIsoDate() {
		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime converted = this.conversionService.convert(
				DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now), OffsetDateTime.class);
		assertThat(converted).isEqualTo(now);
	}

	@Test
	public void convertShouldConvertIsoDateToString() {
		OffsetDateTime now = OffsetDateTime.now();
		String converted = this.conversionService.convert(now, String.class);
		assertThat(converted).isNotNull()
				.startsWith(now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}

	@Parameters(name = "{0}")
	public static Iterable<Object[]> conversionServices() {
		return new ConversionServiceParameters(new IsoOffsetFormatter());
	}

}
