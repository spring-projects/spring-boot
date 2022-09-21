/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.aot.hint.TypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * A supplier for {@link ClientHttpRequestFactory} that detects the preferred candidate
 * based on the available implementations on the classpath.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @since 2.1.0
 */
public class ClientHttpRequestFactorySupplier implements Supplier<ClientHttpRequestFactory> {

	private static final String APACHE_HTTP_CLIENT_CLASS = "org.apache.hc.client5.http.impl.classic.HttpClients";

	private static final boolean APACHE_HTTP_CLIENT_PRESENT = ClassUtils.isPresent(APACHE_HTTP_CLIENT_CLASS, null);

	private static final String OKHTTP_CLIENT_CLASS = "okhttp3.OkHttpClient";

	private static final boolean OKHTTP_CLIENT_PRESENT = ClassUtils.isPresent(OKHTTP_CLIENT_CLASS, null);

	@Override
	public ClientHttpRequestFactory get() {
		if (APACHE_HTTP_CLIENT_PRESENT) {
			return new HttpComponentsClientHttpRequestFactory();
		}
		if (OKHTTP_CLIENT_PRESENT) {
			return new OkHttp3ClientHttpRequestFactory();
		}
		return new SimpleClientHttpRequestFactory();
	}

	static class ClientHttpRequestFactorySupplierRuntimeHints {

		static void registerHints(RuntimeHints hints, ClassLoader classLoader, Consumer<Builder> callback) {
			if (ClassUtils.isPresent(APACHE_HTTP_CLIENT_CLASS, classLoader)) {
				hints.reflection().registerType(HttpComponentsClientHttpRequestFactory.class, (typeHint) -> callback
						.accept(typeHint.onReachableType(TypeReference.of(APACHE_HTTP_CLIENT_CLASS))));
			}
			if (ClassUtils.isPresent(OKHTTP_CLIENT_CLASS, classLoader)) {
				hints.reflection().registerType(OkHttp3ClientHttpRequestFactory.class,
						(typeHint) -> callback.accept(typeHint.onReachableType(TypeReference.of(OKHTTP_CLIENT_CLASS))));
			}
			hints.reflection().registerType(SimpleClientHttpRequestFactory.class, (typeHint) -> callback
					.accept(typeHint.onReachableType(TypeReference.of(SimpleClientHttpRequestFactory.class))));
		}

	}

}
