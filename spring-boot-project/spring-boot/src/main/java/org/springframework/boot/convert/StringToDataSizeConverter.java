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

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

/**
 * {@link Converter} to convert from a {@link String} to a {@link DataSize}. Supports
 * {@link DataSize#parse(CharSequence)}.
 *
 * @author Stephane Nicoll
 * @see DataSizeUnit
 */
final class StringToDataSizeConverter implements GenericConverter {

	/**
	 * Returns a set of convertible types for the StringToDataSizeConverter class.
	 * @return a set containing a single ConvertiblePair object representing the
	 * conversion from String to DataSize.
	 */
	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, DataSize.class));
	}

	/**
	 * Converts the given source object to the specified target type.
	 * @param source the source object to be converted
	 * @param sourceType the TypeDescriptor of the source object
	 * @param targetType the TypeDescriptor of the target type
	 * @return the converted object of the target type, or null if the source object is
	 * empty
	 */
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ObjectUtils.isEmpty(source)) {
			return null;
		}
		return convert(source.toString(), getDataUnit(targetType));
	}

	/**
	 * Retrieves the data unit associated with the given target type.
	 * @param targetType the type descriptor of the target type
	 * @return the data unit specified by the {@link DataSizeUnit} annotation on the
	 * target type, or null if the annotation is not present
	 */
	private DataUnit getDataUnit(TypeDescriptor targetType) {
		DataSizeUnit annotation = targetType.getAnnotation(DataSizeUnit.class);
		return (annotation != null) ? annotation.value() : null;
	}

	/**
	 * Converts a string representation of data size to a DataSize object.
	 * @param source the string representation of the data size
	 * @param unit the unit of the data size
	 * @return the DataSize object representing the converted data size
	 */
	private DataSize convert(String source, DataUnit unit) {
		return DataSize.parse(source, unit);
	}

}
