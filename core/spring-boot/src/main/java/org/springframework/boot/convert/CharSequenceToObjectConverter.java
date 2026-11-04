/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.convert;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ObjectUtils;

/**
 * {@link ConditionalGenericConverter} to convert {@link CharSequence} type by delegating
 * to existing {@link String} converters.
 *
 * @author Phillip Webb
 */
class CharSequenceToObjectConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STRING = TypeDescriptor.valueOf(String.class);

	private static final TypeDescriptor BYTE_ARRAY = TypeDescriptor.valueOf(byte[].class);

	private static final Set<ConvertiblePair> TYPES;

	static {
		TYPES = Collections.singleton(new ConvertiblePair(CharSequence.class, Object.class));
	}

	private final ConversionService conversionService;

	CharSequenceToObjectConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType() == String.class || sourceType instanceof ProtectedTypeDescriptor) {
			return false;
		}
		sourceType = new ProtectedTypeDescriptor(sourceType);
		boolean canDirectlyConvertCharSequence = this.conversionService.canConvert(sourceType, targetType);
		if (canDirectlyConvertCharSequence && !isStringConversionBetter(sourceType, targetType)) {
			return false;
		}
		return this.conversionService.canConvert(STRING, targetType);
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
				// If an ObjectTo... converter is being used then there might be a better
				// StringTo... version
				return true;
			}
		}
		if ((targetType.isArray() || targetType.isCollection()) && !targetType.equals(BYTE_ARRAY)) {
			// StringToArrayConverter / StringToCollectionConverter are better than
			// ObjectToArrayConverter / ObjectToCollectionConverter
			return true;
		}
		return false;
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.conversionService.convert((source != null) ? source.toString() : null, STRING, targetType);
	}

	/**
	 * Wrapper for {@link TypeDescriptor} used to protect against infinite recursion and
	 * cache pollution.
	 */
	@CacheProtected
	static class ProtectedTypeDescriptor extends TypeDescriptor {

		ProtectedTypeDescriptor(TypeDescriptor typeDescriptor) {
			super(typeDescriptor.getResolvableType(), null, withCacheDisabled(typeDescriptor.getAnnotations()));
		}

		private static Annotation[] withCacheDisabled(Annotation[] annotations) {
			Annotation[] disabledAnnotations = ProtectedTypeDescriptor.class.getDeclaredAnnotations();
			return (annotations.length == 0) ? disabledAnnotations
					: ObjectUtils.addObjectToArray(annotations, disabledAnnotations[0]);
		}

	}

	/**
	 * Annotation added to {@link ProtectedTypeDescriptor} to ensure a different cache key
	 * is used.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@interface CacheProtected {

	}

}
