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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Converts a {@link Delimiter delimited} String to an Array.
 *
 * @author Phillip Webb
 */
final class DelimitedStringToArrayConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	/**
	 * Constructs a new DelimitedStringToArrayConverter with the specified
	 * ConversionService.
	 * @param conversionService the ConversionService to be used for converting the
	 * delimited string to an array
	 * @throws IllegalArgumentException if the conversionService is null
	 */
	DelimitedStringToArrayConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * Returns a set of convertible types for the DelimitedStringToArrayConverter class.
	 * @return a set containing a single ConvertiblePair object representing the
	 * conversion from String to Object[].
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Object[].class));
	}

	/**
	 * Determines if the source type can be converted to the target type.
	 * @param sourceType the TypeDescriptor of the source type
	 * @param targetType the TypeDescriptor of the target type
	 * @return true if the source type can be converted to the target type, false
	 * otherwise
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return targetType.getElementTypeDescriptor() == null
				|| this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor());
	}

	/**
	 * Converts a delimited string to an array.
	 * @param source the source object to be converted
	 * @param sourceType the TypeDescriptor of the source object
	 * @param targetType the TypeDescriptor of the target object
	 * @return the converted object
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((String) source, sourceType, targetType);
	}

	/**
	 * Converts a delimited string to an array of objects.
	 * @param source the source string to be converted
	 * @param sourceType the type descriptor of the source string
	 * @param targetType the type descriptor of the target array
	 * @return the converted array of objects
	 */
	private Object convert(String source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Delimiter delimiter = targetType.getAnnotation(Delimiter.class);
		String[] elements = getElements(source, (delimiter != null) ? delimiter.value() : ",");
		TypeDescriptor elementDescriptor = targetType.getElementTypeDescriptor();
		Object target = Array.newInstance(elementDescriptor.getType(), elements.length);
		for (int i = 0; i < elements.length; i++) {
			String sourceElement = elements[i];
			Object targetElement = this.conversionService.convert(sourceElement.trim(), sourceType, elementDescriptor);
			Array.set(target, i, targetElement);
		}
		return target;
	}

	/**
	 * Converts a delimited string into an array of elements.
	 * @param source the source string to be converted
	 * @param delimiter the delimiter used to separate the elements in the source string
	 * @return an array of elements extracted from the source string
	 */
	private String[] getElements(String source, String delimiter) {
		return StringUtils.delimitedListToStringArray(source, Delimiter.NONE.equals(delimiter) ? null : delimiter);
	}

}
