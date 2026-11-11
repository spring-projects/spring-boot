/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Internal utility to handle filtering of addresses and possibly throwing a
 * {@link FilteredHostException}.
 *
 * @param <T> the address type
 * @author Phillip Webb
 */
final class FilteredAddresses<T> {

	private final Stream<T> stream;

	private FilteredAddresses(Stream<T> stream) {
		this.stream = stream;
	}

	Filtered<List<T>> toList() {
		return new Filtered<>(this.stream.toList(), List::isEmpty);
	}

	Filtered<T[]> toArray(IntFunction<T[]> generator) {
		return new Filtered<>(this.stream.toArray(generator), ObjectUtils::isEmpty);
	}

	Filtered<T> get() {
		return new Filtered<>(this.stream.findAny().orElse(null), Objects::isNull);
	}

	static <T> FilteredAddresses<T> of(Stream<T> stream, Predicate<? super T> predicate) {
		return new FilteredAddresses<>(stream.filter(Objects::nonNull).filter(predicate));
	}

	static final class Filtered<T> {

		private final @Nullable T result;

		private final Predicate<T> check;

		Filtered(@Nullable T result, Predicate<T> check) {
			this.result = result;
			this.check = check;
		}

		T orElseThrow(String host, InetAddressFilter filter) {
			return orElseThrow(() -> host, filter);
		}

		T orElseThrow(Supplier<String> host, InetAddressFilter filter) {
			if (this.result == null || this.check.test(this.result)) {
				throw new FilteredHostException(host.get(), filter);
			}
			return this.result;
		}

	}

}
