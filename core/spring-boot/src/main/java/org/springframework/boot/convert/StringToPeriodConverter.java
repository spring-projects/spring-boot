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
 * {@link Converter} to convert from a {@link String} to a {@link Period}. Supports
 * {@link Period#parse(CharSequence)} as well a more readable form.
 *
 * @author Eddú Meléndez
 * @author Edson Chávez
 * @see PeriodFormat
 * @see PeriodUnit
 */
final class StringToPeriodConverter implements GenericConverter {

	@Override
	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Period.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert(source.toString(), getStyle(targetType), getPeriodUnit(targetType));
	}

	private @Nullable PeriodStyle getStyle(TypeDescriptor targetType) {
		PeriodFormat annotation = targetType.getAnnotation(PeriodFormat.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private @Nullable ChronoUnit getPeriodUnit(TypeDescriptor targetType) {
		PeriodUnit annotation = targetType.getAnnotation(PeriodUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private Period convert(String source, @Nullable PeriodStyle style, @Nullable ChronoUnit unit) {
		style = (style != null) ? style : PeriodStyle.detect(source);
		return style.parse(source, unit);
	}

}
