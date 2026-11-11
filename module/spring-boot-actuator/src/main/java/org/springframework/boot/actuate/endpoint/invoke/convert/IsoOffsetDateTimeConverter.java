/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke.convert;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.StringUtils;

/**
 * A {@link String} to {@link OffsetDateTime} {@link Converter} that uses
 * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME ISO offset} parsing.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class IsoOffsetDateTimeConverter implements Converter<String, @Nullable OffsetDateTime> {

	@Override
	public @Nullable OffsetDateTime convert(String source) {
		if (StringUtils.hasLength(source)) {
			return OffsetDateTime.parse(source, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		}
		return null;
	}

	public static void registerConverter(ConverterRegistry registry) {
		registry.addConverter(new IsoOffsetDateTimeConverter());
	}

}
