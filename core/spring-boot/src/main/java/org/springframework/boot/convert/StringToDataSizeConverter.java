/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * {@link Converter} to convert from a {@link String} to a {@link DataSize}. Supports
 * {@link DataSize#parse(CharSequence)}.
 *
 * @author Stephane Nicoll
 * @see DataSizeUnit
 */
final class StringToDataSizeConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, DataSize.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert(source.toString(), getDataUnit(targetType));
	}

	private @Nullable DataUnit getDataUnit(TypeDescriptor targetType) {
		DataSizeUnit annotation = targetType.getAnnotation(DataSizeUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	private DataSize convert(String source, @Nullable DataUnit unit) {
		return DataSize.parse(source, unit);
	}

}
