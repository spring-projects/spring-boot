/*
 * Copyright 2012-2023 the original author or authors.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

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

	/**
	 * Returns a set of convertible types for the DurationToNumberConverter class.
	 * @return a set containing a single ConvertiblePair object representing the
	 * conversion from Duration to Number.
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Duration.class, Number.class));
	}

	/**
	 * Converts a Duration object to a Number object.
	 * @param source the source object to be converted
	 * @param sourceType the TypeDescriptor of the source object
	 * @param targetType the TypeDescriptor of the target object
	 * @return the converted object, or null if the source object is null
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((Duration) source, getDurationUnit(sourceType), targetType.getObjectType());
	}

	/**
	 * Returns the duration unit specified by the {@link DurationUnit} annotation on the
	 * given source type.
	 * @param sourceType the source type to retrieve the duration unit from
	 * @return the duration unit specified by the annotation, or null if the annotation is
	 * not present
	 */
	private ChronoUnit getDurationUnit(TypeDescriptor sourceType) {
		DurationUnit annotation = sourceType.getAnnotation(DurationUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	/**
	 * Converts a Duration object to a specified type using the given ChronoUnit.
	 * @param source the Duration object to be converted
	 * @param unit the ChronoUnit to be used for conversion
	 * @param type the target type to convert the Duration to
	 * @return the converted object of the specified type
	 * @throws IllegalStateException if an error occurs during the conversion process
	 */
	private Object convert(Duration source, ChronoUnit unit, Class<?> type) {
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
