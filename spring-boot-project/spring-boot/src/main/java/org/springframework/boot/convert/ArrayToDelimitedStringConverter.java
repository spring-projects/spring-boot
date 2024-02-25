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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.ObjectUtils;

/**
 * Converts an array to a delimited String.
 *
 * @author Phillip Webb
 */
final class ArrayToDelimitedStringConverter implements ConditionalGenericConverter {

	private final CollectionToDelimitedStringConverter delegate;

	/**
	 * Constructs a new ArrayToDelimitedStringConverter with the specified
	 * ConversionService.
	 * @param conversionService the ConversionService to be used for converting elements
	 * in the array
	 */
	ArrayToDelimitedStringConverter(ConversionService conversionService) {
		this.delegate = new CollectionToDelimitedStringConverter(conversionService);
	}

	/**
	 * Returns a set of convertible types for the ArrayToDelimitedStringConverter class.
	 * @return a set containing a single ConvertiblePair object representing the
	 * conversion from Object[] to String.
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, String.class));
	}

	/**
	 * Determines if the given source type and target type can be converted by this
	 * converter.
	 * @param sourceType the source type to be converted
	 * @param targetType the target type to be converted to
	 * @return true if the conversion is possible, false otherwise
	 */
	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.delegate.matches(sourceType, targetType);
	}

	/**
	 * Converts an array to a delimited string.
	 * @param source the source object to be converted
	 * @param sourceType the TypeDescriptor for the source object
	 * @param targetType the TypeDescriptor for the target object
	 * @return the converted object
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		List<Object> list = Arrays.asList(ObjectUtils.toObjectArray(source));
		return this.delegate.convert(list, sourceType, targetType);
	}

}
