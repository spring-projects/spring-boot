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
import java.lang.reflect.Method;
import java.net.HttpURLConnection;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
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
		hints.registerTypeIfPresent(classLoader, ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS, (typeHint) -> {
			typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS));
			registerReflectionHints(hints, HttpComponentsClientHttpRequestFactory.class);
		});
		hints.registerTypeIfPresent(classLoader, ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS, (typeHint) -> {
			typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS));
			registerReflectionHints(hints, OkHttp3ClientHttpRequestFactory.class);
		});
		hints.registerType(SimpleClientHttpRequestFactory.class, (typeHint) -> {
			typeHint.onReachableType(HttpURLConnection.class);
			registerReflectionHints(hints, SimpleClientHttpRequestFactory.class);
		});
	}

	private void registerReflectionHints(ReflectionHints hints,
			Class<? extends ClientHttpRequestFactory> requestFactoryType) {
		registerMethod(hints, requestFactoryType, "setConnectTimeout", int.class);
		registerMethod(hints, requestFactoryType, "setReadTimeout", int.class);
		registerMethod(hints, requestFactoryType, "setBufferRequestBody", boolean.class);
	}

	private void registerMethod(ReflectionHints hints, Class<? extends ClientHttpRequestFactory> requestFactoryType,
			String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(requestFactoryType, methodName, parameterTypes);
		if (method != null) {
			hints.registerMethod(method, ExecutableMode.INVOKE);
		}
	}

	private Field findField(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		Assert.state(field != null, () -> "Unable to find field '%s' on %s".formatted(type.getName(), name));
		return field;
	}

}
