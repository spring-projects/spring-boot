/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import java.util.UUID;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Example {@link Converter} used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Stephane Nicoll
 */
@Component
public class ExampleIdConverter implements Converter<String, ExampleId> {

	@Override
	public ExampleId convert(String source) {
		return new ExampleId(UUID.fromString(source));
	}

}
