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

	private static Map<String, List<String>> ALIASES;

	static {
		MultiValueMap<String, String> aliases = new LinkedMultiValueMap<>();
		aliases.add("true", "on");
		aliases.add("false", "off");
		ALIASES = Collections.unmodifiableMap(aliases);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> Converter<T, E> getConverter(Class<E> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
		return new LenientToEnumConverter<E>((Class<E>) enumType);
	}

	@SuppressWarnings("unchecked")
	private class LenientToEnumConverter<E extends Enum> implements Converter<T, E> {

		private final Class<E> enumType;

		LenientToEnumConverter(Class<E> enumType) {
			this.enumType = enumType;
		}

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

		private String getCanonicalName(String name) {
			StringBuilder canonicalName = new StringBuilder(name.length());
			name.chars().filter(Character::isLetterOrDigit).map(Character::toLowerCase)
					.forEach((c) -> canonicalName.append((char) c));
			return canonicalName.toString();
		}

	}

}
