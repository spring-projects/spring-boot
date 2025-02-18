/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.http.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Internal builder for {@link ClientHttpRequestFactoryBuilder#of(Class)} and
 * {@link ClientHttpRequestFactoryBuilder#of(Supplier)}.
 *
 * @param <T> the {@link ClientHttpRequestFactory} type
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
final class ReflectiveComponentsClientHttpRequestFactoryBuilder<T extends ClientHttpRequestFactory>
		implements ClientHttpRequestFactoryBuilder<T> {

	private final Supplier<T> requestFactorySupplier;

	ReflectiveComponentsClientHttpRequestFactoryBuilder(Supplier<T> requestFactorySupplier) {
		Assert.notNull(requestFactorySupplier, "'requestFactorySupplier' must not be null");
		this.requestFactorySupplier = requestFactorySupplier;
	}

	ReflectiveComponentsClientHttpRequestFactoryBuilder(Class<T> requestFactoryType) {
		Assert.notNull(requestFactoryType, "'requestFactoryType' must not be null");
		this.requestFactorySupplier = () -> createRequestFactory(requestFactoryType);
	}

	private static <T extends ClientHttpRequestFactory> T createRequestFactory(Class<T> requestFactory) {
		try {
			Constructor<T> constructor = requestFactory.getDeclaredConstructor();
			constructor.setAccessible(true);
			return constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public T build(ClientHttpRequestFactorySettings settings) {
		T requestFactory = this.requestFactorySupplier.get();
		if (settings != null) {
			configure(requestFactory, settings);
		}
		return requestFactory;
	}

	private void configure(ClientHttpRequestFactory requestFactory, ClientHttpRequestFactorySettings settings) {
		Assert.state(settings.sslBundle() == null, "Unable to set SSL bundle using reflection");
		Assert.state(settings.redirects() == Redirects.FOLLOW_WHEN_POSSIBLE,
				"Unable to set redirect follow using reflection");
		ClientHttpRequestFactory unwrapped = unwrapRequestFactoryIfNecessary(requestFactory);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).to((connectTimeout) -> setConnectTimeout(unwrapped, connectTimeout));
		map.from(settings::readTimeout).to((readTimeout) -> setReadTimeout(unwrapped, readTimeout));
	}

	private ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(ClientHttpRequestFactory requestFactory) {
		if (!(requestFactory instanceof AbstractClientHttpRequestFactoryWrapper)) {
			return requestFactory;
		}
		Field field = ReflectionUtils.findField(AbstractClientHttpRequestFactoryWrapper.class, "requestFactory");
		ReflectionUtils.makeAccessible(field);
		ClientHttpRequestFactory unwrappedRequestFactory = requestFactory;
		while (unwrappedRequestFactory instanceof AbstractClientHttpRequestFactoryWrapper) {
			unwrappedRequestFactory = (ClientHttpRequestFactory) ReflectionUtils.getField(field,
					unwrappedRequestFactory);
		}
		return unwrappedRequestFactory;
	}

	private void setConnectTimeout(ClientHttpRequestFactory factory, Duration connectTimeout) {
		Method method = tryFindMethod(factory, "setConnectTimeout", Duration.class);
		if (method != null) {
			invoke(factory, method, connectTimeout);
			return;
		}
		method = findMethod(factory, "setConnectTimeout", int.class);
		int timeout = Math.toIntExact(connectTimeout.toMillis());
		invoke(factory, method, timeout);
	}

	private void setReadTimeout(ClientHttpRequestFactory factory, Duration readTimeout) {
		Method method = tryFindMethod(factory, "setReadTimeout", Duration.class);
		if (method != null) {
			invoke(factory, method, readTimeout);
			return;
		}
		method = findMethod(factory, "setReadTimeout", int.class);
		int timeout = Math.toIntExact(readTimeout.toMillis());
		invoke(factory, method, timeout);
	}

	private Method findMethod(ClientHttpRequestFactory requestFactory, String methodName, Class<?>... parameters) {
		Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
		Assert.state(method != null, () -> "Request factory %s does not have a suitable %s method"
			.formatted(requestFactory.getClass().getName(), methodName));
		Assert.state(!method.isAnnotationPresent(Deprecated.class),
				() -> "Request factory %s has the %s method marked as deprecated"
					.formatted(requestFactory.getClass().getName(), methodName));
		return method;
	}

	private Method tryFindMethod(ClientHttpRequestFactory requestFactory, String methodName, Class<?>... parameters) {
		Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
		if (method == null) {
			return null;
		}
		if (method.isAnnotationPresent(Deprecated.class)) {
			return null;
		}
		return method;
	}

	private void invoke(ClientHttpRequestFactory requestFactory, Method method, Object... parameters) {
		ReflectionUtils.invokeMethod(method, requestFactory, parameters);
	}

}
