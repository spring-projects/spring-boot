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

import java.text.ParseException;
import java.util.Collections;
import java.util.Set;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Parser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Converter} to convert from a {@link String} to {@code <T>} using the underlying
 * {@link Parser}{@code <T>}.
 *
 * @author Dmytro Nosan
 * @since 2.2.0
 */
public class ParserConverter implements GenericConverter {

	private final Class<?> type;

	private final Parser<?> parser;

	/**
	 * Creates a {@code Converter} to convert {@code String} to a {@code T} via parser.
	 * @param parser parses {@code String} to a {@code T}
	 */
	public ParserConverter(Parser<?> parser) {
		Assert.notNull(parser, "Parser must not be null");
		this.type = getType(parser);
		this.parser = parser;
	}

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, this.type));
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		String value = (String) source;
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			return this.parser.parse(value, LocaleContextHolder.getLocale());
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException("Value [" + value + "] can not be parsed", ex);
		}
	}

	@Override
	public String toString() {
		return String.class.getName() + " -> " + this.type.getName() + " : " + this.parser;
	}

	private static Class<?> getType(Parser<?> parser) {
		Class<?> type = GenericTypeResolver.resolveTypeArgument(parser.getClass(), Parser.class);
		if (type == null && parser instanceof DecoratingProxy) {
			type = GenericTypeResolver.resolveTypeArgument(((DecoratingProxy) parser).getDecoratedClass(),
					Parser.class);
		}
		if (type == null) {
			throw new IllegalArgumentException("Unable to extract the parameterized type from Parser: '"
					+ parser.getClass().getName() + "'. Does the class parameterize the <T> generic type?");
		}
		return type;
	}

}
