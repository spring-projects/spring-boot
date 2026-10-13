/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ReflectionUtils;

/**
 * {@link Converter} to convert from a {@link Duration} to a {@link Number}.
 *
 * @author Phillip Webb
 * @see DurationFormat
 * @see DurationUnit
 */
final class DurationToNumberConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Duration.class, Number.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((Duration) source, getDurationUnit(sourceType), targetType.getObjectType());
	}

	private @Nullable ChronoUnit getDurationUnit(TypeDescriptor sourceType) {
		DurationUnit annotation = sourceType.getAnnotation(DurationUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private Object convert(Duration source, @Nullable ChronoUnit unit, Class<?> type) {
		try {
			return type.getConstructor(String.class)
				.newInstance(String.valueOf(DurationStyle.Unit.fromChronoUnit(unit).longValue(source)));
		}
		catch (Exception ex) {
			ReflectionUtils.rethrowRuntimeException(ex);
			throw new IllegalStateException(ex);
		}
	}

}
