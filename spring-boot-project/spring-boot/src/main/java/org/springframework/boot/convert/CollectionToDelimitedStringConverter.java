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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a Collection to a delimited String.
 *
 * @author Phillip Webb
 */
final class CollectionToDelimitedStringConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;

	/**
     * Constructs a new CollectionToDelimitedStringConverter with the specified ConversionService.
     *
     * @param conversionService the ConversionService to be used for converting elements in the collection
     */
    CollectionToDelimitedStringConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
     * Returns a set of convertible types for the CollectionToDelimitedStringConverter class.
     * 
     * @return a set containing a single ConvertiblePair object representing the conversion from Collection to String.
     */
    @Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Collection.class, String.class));
	}

	/**
     * Determines if the given source type and target type can be converted by this converter.
     * 
     * @param sourceType the source type descriptor
     * @param targetType the target type descriptor
     * @return true if the conversion is possible, false otherwise
     */
    @Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		TypeDescriptor sourceElementType = sourceType.getElementTypeDescriptor();
		if (targetType == null || sourceElementType == null) {
			return true;
		}
		return this.conversionService.canConvert(sourceElementType, targetType)
				|| sourceElementType.getType().isAssignableFrom(targetType.getType());
	}

	/**
     * Converts a collection to a delimited string.
     * 
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
		Collection<?> sourceCollection = (Collection<?>) source;
		return convert(sourceCollection, sourceType, targetType);
	}

	/**
     * Converts a collection of elements to a delimited string representation.
     * 
     * @param source the collection of elements to be converted
     * @param sourceType the type descriptor of the source collection
     * @param targetType the type descriptor of the target string
     * @return the delimited string representation of the collection elements
     */
    private Object convert(Collection<?> source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source.isEmpty()) {
			return "";
		}
		return source.stream()
			.map((element) -> convertElement(element, sourceType, targetType))
			.collect(Collectors.joining(getDelimiter(sourceType)));
	}

	/**
     * Returns the delimiter specified by the {@link Delimiter} annotation on the given source type.
     * If the annotation is present, the value specified in the annotation is returned.
     * If the annotation is not present, a default delimiter of "," is returned.
     *
     * @param sourceType the source type to retrieve the delimiter for
     * @return the delimiter specified by the {@link Delimiter} annotation, or a default delimiter of ","
     */
    private CharSequence getDelimiter(TypeDescriptor sourceType) {
		Delimiter annotation = sourceType.getAnnotation(Delimiter.class);
		return (annotation != null) ? annotation.value() : ",";
	}

	/**
     * Converts the given element from the source type to the target type and returns it as a string.
     * 
     * @param element the element to be converted
     * @param sourceType the type descriptor of the source type
     * @param targetType the type descriptor of the target type
     * @return the converted element as a string
     */
    private String convertElement(Object element, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return String
			.valueOf(this.conversionService.convert(element, sourceType.elementTypeDescriptor(element), targetType));
	}

}
