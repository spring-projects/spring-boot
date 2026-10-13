/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link Period} to a {@link String}.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @see PeriodFormat
 * @see PeriodUnit
 */
final class PeriodToStringConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Period.class, String.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert((Period) source, getPeriodStyle(sourceType), getPeriodUnit(sourceType));
	}

	private @Nullable PeriodStyle getPeriodStyle(TypeDescriptor sourceType) {
		PeriodFormat annotation = sourceType.getAnnotation(PeriodFormat.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private String convert(Period source, @Nullable PeriodStyle style, @Nullable ChronoUnit unit) {
		style = (style != null) ? style : PeriodStyle.ISO8601;
		return style.print(source, unit);
	}

	private @Nullable ChronoUnit getPeriodUnit(TypeDescriptor sourceType) {
		PeriodUnit annotation = sourceType.getAnnotation(PeriodUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

}
