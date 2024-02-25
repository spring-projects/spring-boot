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
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Converts a {@link Delimiter delimited} String to a Collection.
 *
 * @author Phillip Webb
 */
final class DelimitedStringToCollectionConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	/**
     * Constructs a new DelimitedStringToCollectionConverter with the specified ConversionService.
     * 
     * @param conversionService the ConversionService to be used for converting the delimited string to a collection
     * @throws IllegalArgumentException if the conversionService is null
     */
    DelimitedStringToCollectionConverter(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
     * Returns a set of convertible types for the DelimitedStringToCollectionConverter class.
     * 
     * @return a set containing a single ConvertiblePair object representing the conversion from String to Collection.
     */
    @Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, Collection.class));
	}

	/**
     * Determines if the source type can be converted to the target type.
     * 
     * @param sourceType the TypeDescriptor of the source type
     * @param targetType the TypeDescriptor of the target type
     * @return true if the source type can be converted to the target type, false otherwise
     */
    @Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return targetType.getElementTypeDescriptor() == null
				|| this.conversionService.canConvert(sourceType, targetType.getElementTypeDescriptor());
	}

	/**
     * Converts the given source object to the specified target type.
     * 
     * @param source the source object to be converted
     * @param sourceType the TypeDescriptor of the source object
     * @param targetType the TypeDescriptor of the target type
     * @return the converted object of the target type
     */
    @Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return convert((String) source, sourceType, targetType);
	}

	/**
     * Converts a delimited string to a collection of objects.
     * 
     * @param source the source string to be converted
     * @param sourceType the type descriptor of the source string
     * @param targetType the type descriptor of the target collection
     * @return the converted collection of objects
     */
    private Object convert(String source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Delimiter delimiter = targetType.getAnnotation(Delimiter.class);
		String[] elements = getElements(source, (delimiter != null) ? delimiter.value() : ",");
		TypeDescriptor elementDescriptor = targetType.getElementTypeDescriptor();
		Collection<Object> target = createCollection(targetType, elementDescriptor, elements.length);
		Stream<Object> stream = Arrays.stream(elements).map(String::trim);
		if (elementDescriptor != null) {
			stream = stream.map((element) -> this.conversionService.convert(element, sourceType, elementDescriptor));
		}
		stream.forEach(target::add);
		return target;
	}

	/**
     * Creates a collection of objects based on the given target type, element descriptor, and length.
     * 
     * @param targetType the target type of the collection
     * @param elementDescriptor the type descriptor of the elements in the collection
     * @param length the length of the collection
     * @return a collection of objects
     */
    private Collection<Object> createCollection(TypeDescriptor targetType, TypeDescriptor elementDescriptor,
			int length) {
		return CollectionFactory.createCollection(targetType.getType(),
				(elementDescriptor != null) ? elementDescriptor.getType() : null, length);
	}

	/**
     * Splits the given source string into an array of elements using the specified delimiter.
     * 
     * @param source the source string to be split
     * @param delimiter the delimiter used to split the source string
     * @return an array of elements obtained by splitting the source string using the delimiter
     */
    private String[] getElements(String source, String delimiter) {
		return StringUtils.delimitedListToStringArray(source, Delimiter.NONE.equals(delimiter) ? null : delimiter);
	}

}
