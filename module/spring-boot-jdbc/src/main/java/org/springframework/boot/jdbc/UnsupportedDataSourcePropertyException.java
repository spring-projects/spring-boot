/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import java.util.function.Supplier;

/**
 * {@link RuntimeException} thrown from {@link DataSourceBuilder} when an unsupported
 * property is used.
 *
 * @author Phillip Webb
 * @since 2.5.0
 */
public class UnsupportedDataSourcePropertyException extends RuntimeException {

	UnsupportedDataSourcePropertyException(String message) {
		super(message);
	}

	static void throwIf(boolean test, Supplier<String> message) {
		if (test) {
			throw new UnsupportedDataSourcePropertyException(message.get());
		}
	}

}
