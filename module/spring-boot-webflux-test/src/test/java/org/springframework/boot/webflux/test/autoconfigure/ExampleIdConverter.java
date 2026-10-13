/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.stereotype.Component;

/**
 * Example {@link GenericConverter} used with {@link WebFluxTest @WebFluxTest} tests.
 *
 * @author Stephane Nicoll
 */
@Component
public class ExampleIdConverter implements GenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(String.class, ExampleId.class));
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return new ExampleId(UUID.fromString((String) source));
	}

}
