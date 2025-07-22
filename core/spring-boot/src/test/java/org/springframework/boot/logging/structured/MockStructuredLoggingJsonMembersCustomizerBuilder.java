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
