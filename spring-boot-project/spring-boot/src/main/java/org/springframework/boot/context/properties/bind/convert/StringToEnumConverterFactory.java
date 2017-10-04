/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind.convert;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.Assert;

/**
 * Converts from a String to a {@link java.lang.Enum} by calling searching matching enum
 * names (ignoring case).
 *
 * @author Phillip Webb
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

	@Override
	public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType,
				"The target type " + targetType.getName() + " does not refer to an enum");
		return new StringToEnum(enumType);
	}

	private class StringToEnum<T extends Enum> implements Converter<String, T> {

		private final Class<T> enumType;

		StringToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(String source) {
			if (source.isEmpty()) {
				return null;
			}
			source = source.trim();
			try {
				return (T) Enum.valueOf(this.enumType, source);
			}
			catch (Exception ex) {
				return findEnum(source);
			}
		}

		private T findEnum(String source) {
			String name = getLettersAndDigits(source);
			for (T candidate : (Set<T>) EnumSet.allOf(this.enumType)) {
				if (getLettersAndDigits(candidate.name()).equals(name)) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("No enum constant "
					+ this.enumType.getCanonicalName() + "." + source);
		}

		private String getLettersAndDigits(String name) {
			StringBuilder canonicalName = new StringBuilder(name.length());
			name.chars().map((c) -> (char) c).filter(Character::isLetterOrDigit)
					.map(Character::toLowerCase).forEach(canonicalName::append);
			return canonicalName.toString();
		}

	}

}
