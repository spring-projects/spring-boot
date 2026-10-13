/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Copy of package-private
 * {@code org.springframework.boot.convert.CharSequenceToObjectConverter}, renamed for
 * differentiation.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
final class ConfigurationPropertiesCharSequenceToObjectConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING = TypeDescriptor.valueOf(String.class);

	private static final TypeDescriptor BYTE_ARRAY = TypeDescriptor.valueOf(byte[].class);

	private static final Set<ConvertiblePair> TYPES;

	private final ThreadLocal<Boolean> disable = new ThreadLocal<>();

	static {
		TYPES = Collections.singleton(new ConvertiblePair(CharSequence.class, Object.class));
	}

	private final ConversionService conversionService;

	ConfigurationPropertiesCharSequenceToObjectConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType() == String.class || this.disable.get() == Boolean.TRUE) {
			return false;
		}
		this.disable.set(Boolean.TRUE);
		try {
			boolean canDirectlyConvertCharSequence = this.conversionService.canConvert(sourceType, targetType);
			if (canDirectlyConvertCharSequence && !isStringConversionBetter(sourceType, targetType)) {
				return false;
			}
			return this.conversionService.canConvert(STRING, targetType);
		}
		finally {
			this.disable.remove();
		}
	}

	/**
	 * Return if String based conversion is better based on the target type. This is
	 * required when ObjectTo... conversion produces incorrect results.
	 * @param sourceType the source type to test
	 * @param targetType the target type to test
	 * @return if string conversion is better
	 */
	private boolean isStringConversionBetter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (this.conversionService instanceof ApplicationConversionService applicationConversionService) {
			if (applicationConversionService.isConvertViaObjectSourceType(sourceType, targetType)) {
				// If an ObjectTo... converter is being used then there might be a
				// better StringTo... version
				return true;
			}
		}
		// StringToArrayConverter / StringToCollectionConverter are better than
		// ObjectToArrayConverter / ObjectToCollectionConverter
		return (targetType.isArray() || targetType.isCollection()) && !targetType.equals(BYTE_ARRAY);
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return this.conversionService.convert(source.toString(), STRING, targetType);
	}

}
