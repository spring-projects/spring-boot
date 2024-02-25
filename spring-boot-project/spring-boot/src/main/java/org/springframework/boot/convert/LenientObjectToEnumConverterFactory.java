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

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Abstract base class for converting from a type to a {@link java.lang.Enum}.
 *
 * @param <T> the source type
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@SuppressWarnings("rawtypes")
abstract class LenientObjectToEnumConverterFactory<T> implements ConverterFactory<T, Enum<?>> {

	private static final Map<String, List<String>> ALIASES;

	static {
		MultiValueMap<String, String> aliases = new LinkedMultiValueMap<>();
		aliases.add("true", "on");
		aliases.add("false", "off");
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	/**
     * Returns a converter that converts a given value to the specified enum type.
     * 
     * @param <E> the enum type
     * @param targetType the target enum type
     * @return a converter that converts a given value to the specified enum type
     * @throws IllegalArgumentException if the target type does not refer to an enum
     */
    @Override
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> Converter<T, E> getConverter(Class<E> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return new LenientToEnumConverter<>((Class<E>) enumType);
	}

	/**
     * LenientToEnumConverter class.
     */
    @SuppressWarnings("unchecked")
	private class LenientToEnumConverter<E extends Enum> implements Converter<T, E> {

		private final Class<E> enumType;

		/**
         * Constructs a new LenientToEnumConverter with the specified enum type.
         * 
         * @param enumType the class object representing the enum type
         */
        LenientToEnumConverter(Class<E> enumType) {
			this.enumType = enumType;
		}

		/**
         * Converts a source object of type T to an enum object of type E.
         * 
         * @param source the source object to be converted
         * @return the converted enum object, or null if the source object is empty
         * @throws IllegalArgumentException if the source object cannot be converted to an enum object
         */
        @Override
		public E convert(T source) {
			String value = source.toString().trim();
			if (value.isEmpty()) {
				return null;
			}
			try {
				return (E) Enum.valueOf(this.enumType, value);
			}
			catch (Exception ex) {
				return findEnum(value);
			}
		}

		/**
         * Finds the enum constant with the specified value.
         * 
         * @param value the value to search for
         * @return the enum constant with the specified value
         * @throws IllegalArgumentException if no enum constant is found with the specified value
         */
        private E findEnum(String value) {
			String name = getCanonicalName(value);
			List<String> aliases = ALIASES.getOrDefault(name, Collections.emptyList());
			for (E candidate : (Set<E>) EnumSet.allOf(this.enumType)) {
				String candidateName = getCanonicalName(candidate.name());
				if (name.equals(candidateName) || aliases.contains(candidateName)) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("No enum constant " + this.enumType.getCanonicalName() + "." + value);
		}

		/**
         * Returns the canonical name of a given name.
         * 
         * @param name the name to get the canonical name for
         * @return the canonical name of the given name
         */
        private String getCanonicalName(String name) {
			StringBuilder canonicalName = new StringBuilder(name.length());
			name.chars()
				.filter(Character::isLetterOrDigit)
				.map(Character::toLowerCase)
				.forEach((c) -> canonicalName.append((char) c));
			return canonicalName.toString();
		}

	}

}
