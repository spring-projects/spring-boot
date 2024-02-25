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
import org.springframework.util.ObjectUtils;

/**
 * {@link Converter} to convert from a {@link String} to a {@link Duration}. Supports
 * {@link Duration#parse(CharSequence)} as well a more readable {@code 10s} form.
 *
 * @author Phillip Webb
 * @see DurationFormat
 * @see DurationUnit
 */
final class StringToDurationConverter implements GenericConverter {

	/**
     * Returns a set of convertible types for the StringToDurationConverter class.
     * 
     * @return a set containing a single ConvertiblePair object representing the conversion from String to Duration.
     */
    @Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Duration.class));
	}

	/**
     * Converts the given source object to the specified target type.
     * 
     * @param source the source object to be converted
     * @param sourceType the TypeDescriptor of the source object
     * @param targetType the TypeDescriptor of the target type
     * @return the converted object of the target type, or null if the source object is empty
     */
    @Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert(source.toString(), getStyle(targetType), getDurationUnit(targetType));
	}

	/**
     * Returns the DurationStyle based on the provided TypeDescriptor.
     * 
     * @param targetType the TypeDescriptor to determine the DurationStyle for
     * @return the DurationStyle based on the provided TypeDescriptor, or null if no DurationFormat annotation is present
     */
    private DurationStyle getStyle(TypeDescriptor targetType) {
		DurationFormat annotation = targetType.getAnnotation(DurationFormat.class);
		return (annotation != null) ? annotation.value() : null;
	}

	/**
     * Returns the duration unit specified by the {@link DurationUnit} annotation
     * associated with the given target type.
     * 
     * @param targetType the target type descriptor
     * @return the duration unit specified by the annotation, or null if the annotation is not present
     */
    private ChronoUnit getDurationUnit(TypeDescriptor targetType) {
		DurationUnit annotation = targetType.getAnnotation(DurationUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	/**
     * Converts a string representation of a duration to a Duration object.
     * 
     * @param source the string representation of the duration
     * @param style the style of the duration string (optional, default is detected automatically)
     * @param unit the unit of the resulting Duration object
     * @return the Duration object representing the given string
     */
    private Duration convert(String source, DurationStyle style, ChronoUnit unit) {
		style = (style != null) ? style : DurationStyle.detect(source);
		return style.parse(source, unit);
	}

}
