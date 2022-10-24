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

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.function.Consumer;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for {@link ClientHttpRequestFactories}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ClientHttpRequestFactoriesRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (ClassUtils.isPresent("org.springframework.http.client.ClientHttpRequestFactory", classLoader)) {
			registerHints(hints.reflection(), classLoader);
		}
	}

	private void registerHints(ReflectionHints hints, ClassLoader classLoader) {
		hints.registerField(findField(AbstractClientHttpRequestFactoryWrapper.class, "requestFactory"));
		if (ClassUtils.isPresent(ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS, classLoader)) {
			registerReflectionHints(hints, HttpComponentsClientHttpRequestFactory.class, this::onReachableHttpClient);
		}
		if (ClassUtils.isPresent(ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS, classLoader)) {
			registerReflectionHints(hints, OkHttp3ClientHttpRequestFactory.class, this::onReachableOkHttpClient);
		}
		registerReflectionHints(hints, SimpleClientHttpRequestFactory.class, this::onReachableHttpUrlConnection);
	}

	private void onReachableHttpUrlConnection(TypeHint.Builder typeHint) {
		typeHint.onReachableType(HttpURLConnection.class);
	}

	private void onReachableHttpClient(TypeHint.Builder typeHint) {
		typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS));
	}

	private void onReachableOkHttpClient(TypeHint.Builder typeHint) {
		typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS));
	}

	private void registerReflectionHints(ReflectionHints hints,
			Class<? extends ClientHttpRequestFactory> requestFactoryType, Consumer<TypeHint.Builder> hintCustomizer) {
		hints.registerType(requestFactoryType, (typeHint) -> {
			typeHint.withMethod("setConnectTimeout", TypeReference.listOf(int.class), ExecutableMode.INVOKE);
			typeHint.withMethod("setReadTimeout", TypeReference.listOf(int.class), ExecutableMode.INVOKE);
			typeHint.withMethod("setBufferRequestBody", TypeReference.listOf(boolean.class), ExecutableMode.INVOKE);
			hintCustomizer.accept(typeHint);
		});
	}

	private Field findField(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		Assert.state(field != null, () -> "Unable to find field '%s' on %s".formatted(type.getName(), name));
		return field;
	}

}
