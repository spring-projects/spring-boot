/*
 * Copyright 2002-2020 the original author or authors.
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

import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

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

	/**
     * Returns a set of convertible types for the PeriodToStringConverter class.
     * 
     * @return a set containing a single ConvertiblePair object representing the conversion from Period to String.
     */
    @Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Period.class, String.class));
	}

	/**
     * Converts the given source object to the target type.
     * 
     * @param source the source object to be converted
     * @param sourceType the type descriptor of the source object
     * @param targetType the type descriptor of the target type
     * @return the converted object of the target type, or null if the source object is empty
     */
    @Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert((Period) source, getPeriodStyle(sourceType), getPeriodUnit(sourceType));
	}

	/**
     * Returns the period style based on the given source type.
     * 
     * @param sourceType the type descriptor to determine the period style from
     * @return the period style specified by the PeriodFormat annotation, or null if not found
     */
    private PeriodStyle getPeriodStyle(TypeDescriptor sourceType) {
		PeriodFormat annotation = sourceType.getAnnotation(PeriodFormat.class);
		return (annotation != null) ? annotation.value() : null;
	}

	/**
     * Converts a Period object to a string representation based on the specified style and unit.
     * 
     * @param source the Period object to be converted
     * @param style the style to be used for formatting the string representation (default is ISO8601 if null)
     * @param unit the unit to be used for formatting the string representation
     * @return the string representation of the Period object
     */
    private String convert(Period source, PeriodStyle style, ChronoUnit unit) {
		style = (style != null) ? style : PeriodStyle.ISO8601;
		return style.print(source, unit);
	}

	/**
     * Returns the period unit specified by the {@link PeriodUnit} annotation on the given source type.
     * 
     * @param sourceType the source type to check for the {@link PeriodUnit} annotation
     * @return the period unit specified by the annotation, or null if the annotation is not present
     */
    private ChronoUnit getPeriodUnit(TypeDescriptor sourceType) {
		PeriodUnit annotation = sourceType.getAnnotation(PeriodUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

}
