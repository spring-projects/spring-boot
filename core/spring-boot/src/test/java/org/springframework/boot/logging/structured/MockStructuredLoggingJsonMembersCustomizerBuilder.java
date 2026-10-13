/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import java.util.function.Supplier;

import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer.Builder;

/**
 * Mock {@link StructuredLoggingJsonMembersCustomizer.Builder}.
 *
 * @param <T> the type being written
 * @author Phillip Webb
 */
public class MockStructuredLoggingJsonMembersCustomizerBuilder<T>
		implements StructuredLoggingJsonMembersCustomizer.Builder<T> {

	private final Supplier<StructuredLoggingJsonMembersCustomizer<T>> customizerSupplier;

	public MockStructuredLoggingJsonMembersCustomizerBuilder(
			Supplier<StructuredLoggingJsonMembersCustomizer<T>> customizerSupplier) {
		this.customizerSupplier = customizerSupplier;
	}

	private boolean nested;

	@Override
	public Builder<T> nested(boolean nested) {
		this.nested = nested;
		return this;
	}

	public boolean isNested() {
		return this.nested;
	}

	@Override
	public StructuredLoggingJsonMembersCustomizer<T> build() {
		return this.customizerSupplier.get();
	}

}
