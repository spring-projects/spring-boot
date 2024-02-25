/*
 * Copyright 2012-2019 the original author or authors.
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

/**
 * {@link Converter} to convert from a {@link Duration} to a {@link String}.
 *
 * @author Phillip Webb
 * @see DurationFormat
 * @see DurationUnit
 */
final class DurationToStringConverter implements GenericConverter {

	/**
	 * Returns a set of convertible types for the DurationToStringConverter class.
	 * @return a set containing a single ConvertiblePair object representing the
	 * conversion from Duration to String.
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Duration.class, String.class));
	}

	/**
	 * Converts the given source object to the target type.
	 * @param source the source object to be converted
	 * @param sourceType the type descriptor of the source object
	 * @param targetType the type descriptor of the target type
	 * @return the converted object of the target type
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((Duration) source, getDurationStyle(sourceType), getDurationUnit(sourceType));
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
	 * Returns the duration style based on the provided source type.
	 * @param sourceType the type descriptor of the source object
	 * @return the duration style specified by the DurationFormat annotation, or null if
	 * not found
	 */
	private DurationStyle getDurationStyle(TypeDescriptor sourceType) {
		DurationFormat annotation = sourceType.getAnnotation(DurationFormat.class);
		return (annotation != null) ? annotation.value() : null;
	}

	/**
	 * Converts a Duration object to a string representation based on the specified style
	 * and unit.
	 * @param source the Duration object to be converted
	 * @param style the style to be used for the conversion (default is ISO8601 if null)
	 * @param unit the unit to be used for the conversion
	 * @return the string representation of the Duration object
	 */
	private String convert(Duration source, DurationStyle style, ChronoUnit unit) {
		style = (style != null) ? style : DurationStyle.ISO8601;
		return style.print(source, unit);
	}

}
