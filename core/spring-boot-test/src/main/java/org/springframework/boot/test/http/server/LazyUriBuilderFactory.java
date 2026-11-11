/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.http.server;

import java.net.URI;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * Lazy {@link UriBuilderFactory} that only obtains the delegate on first call.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class LazyUriBuilderFactory implements UriBuilderFactory {

	private final Supplier<UriBuilderFactory> supplier;

	LazyUriBuilderFactory(Supplier<UriBuilderFactory> supplier) {
		this.supplier = SingletonSupplier.of(supplier);
	}

	@Override
	public URI expand(String uriTemplate, Map<String, ? extends @Nullable Object> uriVariables) {
		return delegate().expand(uriTemplate, uriVariables);
	}

	@Override
	public URI expand(String uriTemplate, @Nullable Object... uriVariables) {
		return delegate().expand(uriTemplate, uriVariables);
	}

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return delegate().uriString(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return delegate().builder();
	}

	private UriBuilderFactory delegate() {
		return this.supplier.get();
	}

}
