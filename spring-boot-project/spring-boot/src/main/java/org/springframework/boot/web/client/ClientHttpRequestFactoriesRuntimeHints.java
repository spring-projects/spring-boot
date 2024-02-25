/*
 * Copyright 2012-2023 the original author or authors.
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
import org.springframework.http.client.JettyClientHttpRequestFactory;
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

	/**
	 * Registers hints for the runtime based on the provided hints and class loader. If
	 * the class "org.springframework.http.client.ClientHttpRequestFactory" is present in
	 * the class loader, it registers hints using reflection.
	 * @param hints the runtime hints to register
	 * @param classLoader the class loader to use for checking the presence of the
	 * required class
	 */
	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		if (ClassUtils.isPresent("org.springframework.http.client.ClientHttpRequestFactory", classLoader)) {
			registerHints(hints.reflection(), classLoader);
		}
	}

	/**
	 * Registers the reflection hints for the given {@link ReflectionHints} and
	 * {@link ClassLoader}.
	 * @param hints the {@link ReflectionHints} to register the hints with
	 * @param classLoader the {@link ClassLoader} to use for loading classes
	 */
	private void registerHints(ReflectionHints hints, ClassLoader classLoader) {
		hints.registerField(findField(AbstractClientHttpRequestFactoryWrapper.class, "requestFactory"));
		hints.registerTypeIfPresent(classLoader, ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS, (typeHint) -> {
			typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.APACHE_HTTP_CLIENT_CLASS));
			registerReflectionHints(hints, HttpComponentsClientHttpRequestFactory.class);
		});
		hints.registerTypeIfPresent(classLoader, ClientHttpRequestFactories.JETTY_CLIENT_CLASS, (typeHint) -> {
			typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.JETTY_CLIENT_CLASS));
			registerReflectionHints(hints, JettyClientHttpRequestFactory.class, long.class);
		});
		hints.registerType(SimpleClientHttpRequestFactory.class, (typeHint) -> {
			typeHint.onReachableType(HttpURLConnection.class);
			registerReflectionHints(hints, SimpleClientHttpRequestFactory.class);
		});
		registerOkHttpHints(hints, classLoader);
	}

	/**
	 * Registers OkHttp hints for the given reflection hints and class loader.
	 * @param hints the reflection hints
	 * @param classLoader the class loader
	 * @deprecated This method has been deprecated since version 3.2.0 and will be removed
	 * in a future release.
	 * @since 3.2.0
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "3.2.0", forRemoval = true)
	private void registerOkHttpHints(ReflectionHints hints, ClassLoader classLoader) {
		hints.registerTypeIfPresent(classLoader, ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS, (typeHint) -> {
			typeHint.onReachableType(TypeReference.of(ClientHttpRequestFactories.OKHTTP_CLIENT_CLASS));
			registerReflectionHints(hints, OkHttp3ClientHttpRequestFactory.class);
		});

	}

	/**
	 * Registers reflection hints for the given {@link ReflectionHints} and
	 * {@link ClientHttpRequestFactory} type.
	 * @param hints The {@link ReflectionHints} to register.
	 * @param requestFactoryType The {@link ClientHttpRequestFactory} type to register
	 * reflection hints for.
	 */
	private void registerReflectionHints(ReflectionHints hints,
			Class<? extends ClientHttpRequestFactory> requestFactoryType) {
		registerReflectionHints(hints, requestFactoryType, int.class);
	}

	/**
	 * Registers reflection hints for the specified request factory type and read timeout
	 * type.
	 * @param hints the reflection hints to register
	 * @param requestFactoryType the type of the client HTTP request factory
	 * @param readTimeoutType the type of the read timeout
	 */
	private void registerReflectionHints(ReflectionHints hints,
			Class<? extends ClientHttpRequestFactory> requestFactoryType, Class<?> readTimeoutType) {
		registerMethod(hints, requestFactoryType, "setConnectTimeout", int.class);
		registerMethod(hints, requestFactoryType, "setReadTimeout", readTimeoutType);
	}

	/**
	 * Registers a method with the given reflection hints, request factory type, method
	 * name, and parameter types.
	 * @param hints the reflection hints to register the method with
	 * @param requestFactoryType the type of the client HTTP request factory
	 * @param methodName the name of the method to register
	 * @param parameterTypes the parameter types of the method to register
	 */
	private void registerMethod(ReflectionHints hints, Class<? extends ClientHttpRequestFactory> requestFactoryType,
			String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(requestFactoryType, methodName, parameterTypes);
		if (method != null) {
			hints.registerMethod(method, ExecutableMode.INVOKE);
		}
	}

	/**
	 * Finds a field with the given name in the specified class.
	 * @param type the class in which to search for the field
	 * @param name the name of the field to find
	 * @return the found field
	 * @throws IllegalStateException if the field cannot be found
	 */
	private Field findField(Class<?> type, String name) {
		Field field = ReflectionUtils.findField(type, name);
		Assert.state(field != null, () -> "Unable to find field '%s' on %s".formatted(type.getName(), name));
		return field;
	}

}
