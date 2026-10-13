/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import java.util.UUID;

import org.springframework.core.convert.converter.Converter;

/**
 * An example attribute that requires a {@link Converter}.
 *
 * @author Stephane Nicoll
 */
public class ExampleId {

	private final UUID id;

	ExampleId(UUID id) {
		this.id = id;
	}

	public UUID getId() {
		return this.id;
	}

}
