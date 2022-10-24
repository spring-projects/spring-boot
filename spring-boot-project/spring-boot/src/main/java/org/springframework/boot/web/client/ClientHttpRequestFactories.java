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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class that can be used to create {@link ClientHttpRequestFactory} instances
 * configured using given {@link ClientHttpRequestFactorySettings}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public final class ClientHttpRequestFactories {

	static final String APACHE_HTTP_CLIENT_CLASS = "org.apache.hc.client5.http.impl.classic.HttpClients";

	private static final boolean APACHE_HTTP_CLIENT_PRESENT = ClassUtils.isPresent(APACHE_HTTP_CLIENT_CLASS, null);

	static final String OKHTTP_CLIENT_CLASS = "okhttp3.OkHttpClient";

	private static final boolean OKHTTP_CLIENT_PRESENT = ClassUtils.isPresent(OKHTTP_CLIENT_CLASS, null);

	private ClientHttpRequestFactories() {
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} instance using the most appropriate
	 * implementation.
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory}
	 */
	public static ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		if (APACHE_HTTP_CLIENT_PRESENT) {
			return HttpComponents.get(settings);
		}
		if (OKHTTP_CLIENT_PRESENT) {
			return OkHttp.get(settings);
		}
		return Simple.get(settings);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} of the given type, applying
	 * {@link ClientHttpRequestFactorySettings} using reflection if necessary.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactoryType the {@link ClientHttpRequestFactory} type
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory} instance
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ClientHttpRequestFactory> T get(Class<T> requestFactoryType,
			ClientHttpRequestFactorySettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		if (requestFactoryType == ClientHttpRequestFactory.class) {
			return (T) get(settings);
		}
		if (requestFactoryType == HttpComponentsClientHttpRequestFactory.class) {
			return (T) HttpComponents.get(settings);
		}
		if (requestFactoryType == OkHttp3ClientHttpRequestFactory.class) {
			return (T) OkHttp.get(settings);
		}
		if (requestFactoryType == SimpleClientHttpRequestFactory.class) {
			return (T) Simple.get(settings);
		}
		return get(() -> createRequestFactory(requestFactoryType), settings);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactory} from the given supplier, applying
	 * {@link ClientHttpRequestFactorySettings} using reflection.
	 * @param <T> the {@link ClientHttpRequestFactory} type
	 * @param requestFactorySupplier the {@link ClientHttpRequestFactory} supplier
	 * @param settings the settings to apply
	 * @return a new {@link ClientHttpRequestFactory} instance
	 */
	public static <T extends ClientHttpRequestFactory> T get(Supplier<T> requestFactorySupplier,
			ClientHttpRequestFactorySettings settings) {
		return Reflective.get(requestFactorySupplier, settings);
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

	/**
	 * Support for {@link HttpComponentsClientHttpRequestFactory}.
	 */
	static class HttpComponents {

		static HttpComponentsClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			HttpComponentsClientHttpRequestFactory requestFactory = createRequestFactory(settings.readTimeout());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			map.from(settings::bufferRequestBody).to(requestFactory::setBufferRequestBody);
			return requestFactory;
		}

		private static HttpComponentsClientHttpRequestFactory createRequestFactory(Duration readTimeout) {
			return (readTimeout != null) ? new HttpComponentsClientHttpRequestFactory(createHttpClient(readTimeout))
					: new HttpComponentsClientHttpRequestFactory();
		}

		private static HttpClient createHttpClient(Duration readTimeout) {
			SocketConfig socketConfig = SocketConfig.custom()
					.setSoTimeout((int) readTimeout.toMillis(), TimeUnit.MILLISECONDS).build();
			PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
					.setDefaultSocketConfig(socketConfig).build();
			return HttpClientBuilder.create().setConnectionManager(connectionManager).build();
		}

	}

	/**
	 * Support for {@link OkHttp3ClientHttpRequestFactory}.
	 */
	static class OkHttp {

		static OkHttp3ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			Assert.state(settings.bufferRequestBody() == null,
					() -> "OkHttp3ClientHttpRequestFactory does not support request body buffering");
			OkHttp3ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
			return requestFactory;
		}

	}

	/**
	 * Support for {@link SimpleClientHttpRequestFactory}.
	 */
	static class Simple {

		static SimpleClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
			SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
			map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
			map.from(settings::bufferRequestBody).to(requestFactory::setBufferRequestBody);
			return requestFactory;
		}

	}

	/**
	 * Support for reflective configuration of an unknown {@link ClientHttpRequestFactory}
	 * implementation.
	 */
	static class Reflective {

		static <T extends ClientHttpRequestFactory> T get(Supplier<T> requestFactorySupplier,
				ClientHttpRequestFactorySettings settings) {
			T requestFactory = requestFactorySupplier.get();
			configure(requestFactory, settings);
			return requestFactory;
		}

		private static void configure(ClientHttpRequestFactory requestFactory,
				ClientHttpRequestFactorySettings settings) {
			ClientHttpRequestFactory unwrapped = unwrapRequestFactoryIfNecessary(requestFactory);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(settings::connectTimeout).to((connectTimeout) -> setConnectTimeout(unwrapped, connectTimeout));
			map.from(settings::readTimeout).to((readTimeout) -> setReadTimeout(unwrapped, readTimeout));
			map.from(settings::bufferRequestBody)
					.to((bufferRequestBody) -> setBufferRequestBody(unwrapped, bufferRequestBody));
		}

		private static ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(
				ClientHttpRequestFactory requestFactory) {
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

		private static void setConnectTimeout(ClientHttpRequestFactory factory, Duration connectTimeout) {
			Method method = findMethod(factory, "setConnectTimeout", int.class);
			int timeout = Math.toIntExact(connectTimeout.toMillis());
			invoke(factory, method, timeout);
		}

		private static void setReadTimeout(ClientHttpRequestFactory factory, Duration readTimeout) {
			Method method = findMethod(factory, "setReadTimeout", int.class);
			int timeout = Math.toIntExact(readTimeout.toMillis());
			invoke(factory, method, timeout);
		}

		private static void setBufferRequestBody(ClientHttpRequestFactory factory, boolean bufferRequestBody) {
			Method method = findMethod(factory, "setBufferRequestBody", boolean.class);
			invoke(factory, method, bufferRequestBody);
		}

		private static Method findMethod(ClientHttpRequestFactory requestFactory, String methodName,
				Class<?>... parameters) {
			Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
			Assert.state(method != null, () -> "Request factory %s does not have a suitable %s method"
					.formatted(requestFactory.getClass().getName(), methodName));
			Assert.state(!method.isAnnotationPresent(Deprecated.class),
					() -> "Request factory %s has the %s method marked as deprecated"
							.formatted(requestFactory.getClass().getName(), methodName));
			return method;
		}

		private static void invoke(ClientHttpRequestFactory requestFactory, Method method, Object... parameters) {
			ReflectionUtils.invokeMethod(method, requestFactory, parameters);
		}

	}

}
