/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.convert;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.StringUtils;

/**
 * A {@link String} to {@link Date} {@link Converter} that uses
 * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME ISO offset} parsing.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class IsoOffsetDateTimeConverter implements Converter<String, Date> {

	@Override
	public Date convert(String source) {
		if (StringUtils.hasLength(source)) {
			OffsetDateTime offsetDateTime = OffsetDateTime.parse(source,
					DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			return new Date(TimeUnit.SECONDS.toMillis(offsetDateTime.toEpochSecond()));
		}
		return null;
	}

	public static void registerConverter(ConverterRegistry registry) {
		registry.addConverter(new IsoOffsetDateTimeConverter());
	}

}
