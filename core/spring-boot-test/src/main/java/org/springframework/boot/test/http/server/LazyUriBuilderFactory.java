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
