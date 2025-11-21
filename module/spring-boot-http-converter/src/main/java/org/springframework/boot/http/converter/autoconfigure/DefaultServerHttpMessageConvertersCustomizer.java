/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.converter.autoconfigure;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ServerBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;

@SuppressWarnings("deprecation")
class DefaultServerHttpMessageConvertersCustomizer implements ServerHttpMessageConvertersCustomizer {

	private final @Nullable HttpMessageConverters legacyConverters;

	private final Collection<HttpMessageConverter<?>> converters;

	DefaultServerHttpMessageConvertersCustomizer(@Nullable HttpMessageConverters legacyConverters,
			Collection<HttpMessageConverter<?>> converters) {

		this.legacyConverters = legacyConverters;
		this.converters = converters;
	}

	@Override
	public void customize(ServerBuilder builder) {
		if (this.legacyConverters != null) {
			this.legacyConverters.forEach(builder::addCustomConverter);
		}
		else {
			builder.registerDefaults();
			EnumSet<ConverterType> registered = EnumSet.noneOf(ConverterType.class);
			for (HttpMessageConverter<?> converter : this.converters) {
				ConverterType type = findConverterType(converter);
				if (type != null) {
					if (!registered.contains(type)) {
						type.registerWith(builder, converter);
						registered.add(type);
					}
					else {
						builder.addCustomConverter(converter);
					}
				}
				else {
					builder.addCustomConverter(converter);
				}
			}
		}
	}

	private static @Nullable ConverterType findConverterType(HttpMessageConverter<?> converter) {
		for (ConverterType type : ConverterType.values()) {
			if (type.matches(converter)) {
				return type;
			}
		}
		return null;
	}

	private static boolean supportsMediaType(HttpMessageConverter<?> converter, MediaType mediaType) {
		for (MediaType supportedMediaType : converter.getSupportedMediaTypes()) {
			if (supportedMediaType.equalsTypeAndSubtype(mediaType)) {
				return true;
			}
		}
		return false;
	}

	private enum ConverterType {

		STRING(StringHttpMessageConverter.class::isInstance, ServerBuilder::withStringConverter),

		KOTLIN_SERIALIZATION_JSON(KotlinSerializationJsonHttpMessageConverter.class::isInstance,
				ServerBuilder::withKotlinSerializationJsonConverter),

		JSON(converter -> supportsMediaType(converter, MediaType.APPLICATION_JSON),
				ServerBuilder::withJsonConverter),

		XML(converter -> supportsMediaType(converter, MediaType.APPLICATION_XML), ServerBuilder::withXmlConverter);

		private final Predicate<HttpMessageConverter<?>> matcher;

		private final BiConsumer<ServerBuilder, HttpMessageConverter<?>> registrar;

		ConverterType(Predicate<HttpMessageConverter<?>> matcher,
				BiConsumer<ServerBuilder, HttpMessageConverter<?>> registrar) {
			this.matcher = matcher;
			this.registrar = registrar;
		}

		boolean matches(HttpMessageConverter<?> converter) {
			return this.matcher.test(converter);
		}

		void registerWith(ServerBuilder builder, HttpMessageConverter<?> converter) {
			this.registrar.accept(builder, converter);
		}

	}

}
