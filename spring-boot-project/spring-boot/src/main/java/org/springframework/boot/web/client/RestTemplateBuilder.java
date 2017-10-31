/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Builder that can be used to configure and create a {@link RestTemplate}. Provides
 * convenience methods to register {@link #messageConverters(HttpMessageConverter...)
 * converters}, {@link #errorHandler(ResponseErrorHandler) error handlers} and
 * {@link #uriTemplateHandler(UriTemplateHandler) UriTemplateHandlers}.
 * <p>
 * By default the built {@link RestTemplate} will attempt to use the most suitable
 * {@link ClientHttpRequestFactory}, call {@link #detectRequestFactory(boolean)
 * detectRequestFactory(false)} if you prefer to keep the default. In a typical
 * auto-configured Spring Boot application this builder is available as a bean and can be
 * injected whenever a {@link RestTemplate} is needed.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class RestTemplateBuilder {

	private static final Map<String, String> REQUEST_FACTORY_CANDIDATES;

	static {
		Map<String, String> candidates = new LinkedHashMap<>();
		candidates.put("org.apache.http.client.HttpClient",
				"org.springframework.http.client.HttpComponentsClientHttpRequestFactory");
		candidates.put("okhttp3.OkHttpClient",
				"org.springframework.http.client.OkHttp3ClientHttpRequestFactory");
		candidates.put("com.squareup.okhttp.OkHttpClient",
				"org.springframework.http.client.OkHttpClientHttpRequestFactory");
		REQUEST_FACTORY_CANDIDATES = Collections.unmodifiableMap(candidates);
	}

	private final boolean detectRequestFactory;

	private final String rootUri;

	private final Set<HttpMessageConverter<?>> messageConverters;

	private final ClientHttpRequestFactory requestFactory;

	private final UriTemplateHandler uriTemplateHandler;

	private final ResponseErrorHandler errorHandler;

	private final BasicAuthorizationInterceptor basicAuthorization;

	private final Set<RestTemplateCustomizer> restTemplateCustomizers;

	private final Set<RequestFactoryCustomizer> requestFactoryCustomizers;

	private final Set<ClientHttpRequestInterceptor> interceptors;

	/**
	 * Create a new {@link RestTemplateBuilder} instance.
	 * @param customizers any {@link RestTemplateCustomizer RestTemplateCustomizers} that
	 * should be applied when the {@link RestTemplate} is built
	 */
	public RestTemplateBuilder(RestTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.detectRequestFactory = true;
		this.rootUri = null;
		this.messageConverters = null;
		this.requestFactory = null;
		this.uriTemplateHandler = null;
		this.errorHandler = null;
		this.basicAuthorization = null;
		this.restTemplateCustomizers = Collections
				.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(customizers)));
		this.requestFactoryCustomizers = Collections.emptySet();
		this.interceptors = Collections.emptySet();
	}

	private RestTemplateBuilder(boolean detectRequestFactory, String rootUri,
			Set<HttpMessageConverter<?>> messageConverters,
			ClientHttpRequestFactory requestFactory,
			UriTemplateHandler uriTemplateHandler, ResponseErrorHandler errorHandler,
			BasicAuthorizationInterceptor basicAuthorization,
			Set<RestTemplateCustomizer> restTemplateCustomizers,
			Set<RequestFactoryCustomizer> requestFactoryCustomizers,
			Set<ClientHttpRequestInterceptor> interceptors) {
		super();
		this.detectRequestFactory = detectRequestFactory;
		this.rootUri = rootUri;
		this.messageConverters = messageConverters;
		this.requestFactory = requestFactory;
		this.uriTemplateHandler = uriTemplateHandler;
		this.errorHandler = errorHandler;
		this.basicAuthorization = basicAuthorization;
		this.restTemplateCustomizers = restTemplateCustomizers;
		this.requestFactoryCustomizers = requestFactoryCustomizers;
		this.interceptors = interceptors;
	}

	/**
	 * Set if the {@link ClientHttpRequestFactory} should be detected based on the
	 * classpath. Default if {@code true}.
	 * @param detectRequestFactory if the {@link ClientHttpRequestFactory} should be
	 * detected
	 * @return a new builder instance
	 */
	public RestTemplateBuilder detectRequestFactory(boolean detectRequestFactory) {
		return new RestTemplateBuilder(detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Set a root URL that should be applied to each request that starts with {@code '/'}.
	 * See {@link RootUriTemplateHandler} for details.
	 * @param rootUri the root URI or {@code null}
	 * @return a new builder instance
	 */
	public RestTemplateBuilder rootUri(String rootUri) {
		return new RestTemplateBuilder(this.detectRequestFactory, rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate}. Setting this value will replace any previously configured
	 * converters.
	 * @param messageConverters the converters to set
	 * @return a new builder instance
	 * @see #additionalMessageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder messageConverters(
			HttpMessageConverter<?>... messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return messageConverters(Arrays.asList(messageConverters));
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate}. Setting this value will replace any previously configured
	 * converters.
	 * @param messageConverters the converters to set
	 * @return a new builder instance
	 * @see #additionalMessageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder messageConverters(
			Collection<? extends HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				Collections.unmodifiableSet(
						new LinkedHashSet<HttpMessageConverter<?>>(messageConverters)),
				this.requestFactory, this.uriTemplateHandler, this.errorHandler,
				this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
	 * used with the {@link RestTemplate}.
	 * @param messageConverters the converters to add
	 * @return a new builder instance
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder additionalMessageConverters(
			HttpMessageConverter<?>... messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return additionalMessageConverters(Arrays.asList(messageConverters));
	}

	/**
	 * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
	 * used with the {@link RestTemplate}.
	 * @param messageConverters the converters to add
	 * @return a new builder instance
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder additionalMessageConverters(
			Collection<? extends HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				append(this.messageConverters, messageConverters), this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthorization,
				this.restTemplateCustomizers, this.requestFactoryCustomizers,
				this.interceptors);
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate} to the default set. Calling this method will replace any
	 * previously defined converters.
	 * @return a new builder instance
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder defaultMessageConverters() {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				Collections.unmodifiableSet(
						new LinkedHashSet<>(new RestTemplate().getMessageConverters())),
				this.requestFactory, this.uriTemplateHandler, this.errorHandler,
				this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Set the {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors} that
	 * should be used with the {@link RestTemplate}. Setting this value will replace any
	 * previously defined interceptors.
	 * @param interceptors the interceptors to set
	 * @return a new builder instance
	 * @since 1.4.1
	 * @see #additionalInterceptors(ClientHttpRequestInterceptor...)
	 */
	public RestTemplateBuilder interceptors(
			ClientHttpRequestInterceptor... interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return interceptors(Arrays.asList(interceptors));
	}

	/**
	 * Set the {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors} that
	 * should be used with the {@link RestTemplate}. Setting this value will replace any
	 * previously defined interceptors.
	 * @param interceptors the interceptors to set
	 * @return a new builder instance
	 * @since 1.4.1
	 * @see #additionalInterceptors(ClientHttpRequestInterceptor...)
	 */
	public RestTemplateBuilder interceptors(
			Collection<ClientHttpRequestInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers,
				Collections.unmodifiableSet(new LinkedHashSet<>(interceptors)));
	}

	/**
	 * Add additional {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}
	 * that should be used with the {@link RestTemplate}.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @since 1.4.1
	 * @see #interceptors(ClientHttpRequestInterceptor...)
	 */
	public RestTemplateBuilder additionalInterceptors(
			ClientHttpRequestInterceptor... interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return additionalInterceptors(Arrays.asList(interceptors));
	}

	/**
	 * Add additional {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}
	 * that should be used with the {@link RestTemplate}.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @since 1.4.1
	 * @see #interceptors(ClientHttpRequestInterceptor...)
	 */
	public RestTemplateBuilder additionalInterceptors(
			Collection<? extends ClientHttpRequestInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, append(this.interceptors, interceptors));
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} class that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactory the request factory to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder requestFactory(
			Class<? extends ClientHttpRequestFactory> requestFactory) {
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		return requestFactory(createRequestFactory(requestFactory));
	}

	private ClientHttpRequestFactory createRequestFactory(
			Class<? extends ClientHttpRequestFactory> requestFactory) {
		try {
			Constructor<?> constructor = requestFactory.getDeclaredConstructor();
			constructor.setAccessible(true);
			return (ClientHttpRequestFactory) constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactory the request factory to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder requestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Set the {@link UriTemplateHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param uriTemplateHandler the URI template handler to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder uriTemplateHandler(UriTemplateHandler uriTemplateHandler) {
		Assert.notNull(uriTemplateHandler, "UriTemplateHandler must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Set the {@link ResponseErrorHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param errorHandler the error handler to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder errorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Add HTTP basic authentication to requests. See
	 * {@link BasicAuthorizationInterceptor} for details.
	 * @param username the user name
	 * @param password the password
	 * @return a new builder instance
	 */
	public RestTemplateBuilder basicAuthorization(String username, String password) {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, new BasicAuthorizationInterceptor(username, password),
				this.restTemplateCustomizers, this.requestFactoryCustomizers,
				this.interceptors);
	}

	/**
	 * Set the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
	 * applied to the {@link RestTemplate}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied. Setting this value will
	 * replace any previously configured customizers.
	 * @param restTemplateCustomizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder customizers(
			RestTemplateCustomizer... restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		return customizers(Arrays.asList(restTemplateCustomizers));
	}

	/**
	 * Set the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
	 * applied to the {@link RestTemplate}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied. Setting this value will
	 * replace any previously configured customizers.
	 * @param restTemplateCustomizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder customizers(
			Collection<? extends RestTemplateCustomizer> restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization,
				Collections.unmodifiableSet(new LinkedHashSet<RestTemplateCustomizer>(
						restTemplateCustomizers)),
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Add {@link RestTemplateCustomizer RestTemplateCustomizers} that should be applied
	 * to the {@link RestTemplate}. Customizers are applied in the order that they were
	 * added after builder configuration has been applied.
	 * @param restTemplateCustomizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder additionalCustomizers(
			RestTemplateCustomizer... restTemplateCustomizers) {
		Assert.notNull(restTemplateCustomizers,
				"RestTemplateCustomizers must not be null");
		return additionalCustomizers(Arrays.asList(restTemplateCustomizers));
	}

	/**
	 * Add {@link RestTemplateCustomizer RestTemplateCustomizers} that should be applied
	 * to the {@link RestTemplate}. Customizers are applied in the order that they were
	 * added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder additionalCustomizers(
			Collection<? extends RestTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "RestTemplateCustomizers must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization,
				append(this.restTemplateCustomizers, customizers),
				this.requestFactoryCustomizers, this.interceptors);
	}

	/**
	 * Sets the connect timeout in milliseconds on the underlying
	 * {@link ClientHttpRequestFactory}.
	 * @param connectTimeout the connect timeout in milliseconds
	 * @return a new builder instance.
	 */
	public RestTemplateBuilder setConnectTimeout(int connectTimeout) {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				append(this.requestFactoryCustomizers,
						new ConnectTimeoutRequestFactoryCustomizer(connectTimeout)),
				this.interceptors);
	}

	/**
	 * Sets the read timeout in milliseconds on the underlying
	 * {@link ClientHttpRequestFactory}.
	 * @param readTimeout the read timeout in milliseconds
	 * @return a new builder instance.
	 */
	public RestTemplateBuilder setReadTimeout(int readTimeout) {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthorization, this.restTemplateCustomizers,
				append(this.requestFactoryCustomizers,
						new ReadTimeoutRequestFactoryCustomizer(readTimeout)),
				this.interceptors);
	}

	/**
	 * Build a new {@link RestTemplate} instance and configure it using this builder.
	 * @return a configured {@link RestTemplate} instance.
	 * @see #build(Class)
	 * @see #configure(RestTemplate)
	 */
	public RestTemplate build() {
		return build(RestTemplate.class);
	}

	/**
	 * Build a new {@link RestTemplate} instance of the specified type and configure it
	 * using this builder.
	 * @param <T> the type of rest template
	 * @param restTemplateClass the template type to create
	 * @return a configured {@link RestTemplate} instance.
	 * @see RestTemplateBuilder#build()
	 * @see #configure(RestTemplate)
	 */

	public <T extends RestTemplate> T build(Class<T> restTemplateClass) {
		return configure(BeanUtils.instantiateClass(restTemplateClass));
	}

	/**
	 * Configure the provided {@link RestTemplate} instance using this builder.
	 * @param <T> the type of rest template
	 * @param restTemplate the {@link RestTemplate} to configure
	 * @return the rest template instance
	 * @see RestTemplateBuilder#build()
	 * @see RestTemplateBuilder#build(Class)
	 */
	public <T extends RestTemplate> T configure(T restTemplate) {
		configureRequestFactory(restTemplate);
		if (!CollectionUtils.isEmpty(this.messageConverters)) {
			restTemplate.setMessageConverters(new ArrayList<>(this.messageConverters));
		}
		if (this.uriTemplateHandler != null) {
			restTemplate.setUriTemplateHandler(this.uriTemplateHandler);
		}
		if (this.errorHandler != null) {
			restTemplate.setErrorHandler(this.errorHandler);
		}
		if (this.rootUri != null) {
			RootUriTemplateHandler.addTo(restTemplate, this.rootUri);
		}
		if (this.basicAuthorization != null) {
			restTemplate.getInterceptors().add(this.basicAuthorization);
		}
		if (!CollectionUtils.isEmpty(this.restTemplateCustomizers)) {
			for (RestTemplateCustomizer customizer : this.restTemplateCustomizers) {
				customizer.customize(restTemplate);
			}
		}
		restTemplate.getInterceptors().addAll(this.interceptors);
		return restTemplate;
	}

	private void configureRequestFactory(RestTemplate restTemplate) {
		ClientHttpRequestFactory requestFactory = null;
		if (this.requestFactory != null) {
			requestFactory = this.requestFactory;
		}
		else if (this.detectRequestFactory) {
			requestFactory = detectRequestFactory();
		}
		if (requestFactory != null) {
			ClientHttpRequestFactory unwrappedRequestFactory = unwrapRequestFactoryIfNecessary(
					requestFactory);
			for (RequestFactoryCustomizer customizer : this.requestFactoryCustomizers) {
				customizer.customize(unwrappedRequestFactory);
			}
			restTemplate.setRequestFactory(requestFactory);
		}
	}

	private ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(
			ClientHttpRequestFactory requestFactory) {
		if (!(requestFactory instanceof AbstractClientHttpRequestFactoryWrapper)) {
			return requestFactory;
		}
		ClientHttpRequestFactory unwrappedRequestFactory = requestFactory;
		Field field = ReflectionUtils.findField(
				AbstractClientHttpRequestFactoryWrapper.class, "requestFactory");
		ReflectionUtils.makeAccessible(field);
		do {
			unwrappedRequestFactory = (ClientHttpRequestFactory) ReflectionUtils
					.getField(field, unwrappedRequestFactory);
		}
		while (unwrappedRequestFactory instanceof AbstractClientHttpRequestFactoryWrapper);
		return unwrappedRequestFactory;
	}

	private ClientHttpRequestFactory detectRequestFactory() {
		for (Map.Entry<String, String> candidate : REQUEST_FACTORY_CANDIDATES
				.entrySet()) {
			ClassLoader classLoader = getClass().getClassLoader();
			if (ClassUtils.isPresent(candidate.getKey(), classLoader)) {
				Class<?> factoryClass = ClassUtils.resolveClassName(candidate.getValue(),
						classLoader);
				return (ClientHttpRequestFactory) BeanUtils
						.instantiateClass(factoryClass);
			}
		}
		return new SimpleClientHttpRequestFactory();
	}

	private <T> Set<T> append(Set<T> set, T addition) {
		Set<T> result = new LinkedHashSet<>(set == null ? Collections.emptySet() : set);
		result.add(addition);
		return Collections.unmodifiableSet(result);
	}

	private <T> Set<T> append(Set<T> set, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>(set == null ? Collections.emptySet() : set);
		result.addAll(additions);
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Strategy interface used to customize the {@link ClientHttpRequestFactory}.
	 */
	private interface RequestFactoryCustomizer {

		void customize(ClientHttpRequestFactory factory);

	}

	/**
	 * {@link RequestFactoryCustomizer} to call a "set timeout" method.
	 */
	private static abstract class TimeoutRequestFactoryCustomizer
			implements RequestFactoryCustomizer {

		private final int timeout;

		private final String methodName;

		TimeoutRequestFactoryCustomizer(int timeout, String methodName) {
			this.timeout = timeout;
			this.methodName = methodName;
		}

		@Override
		public void customize(ClientHttpRequestFactory factory) {
			ReflectionUtils.invokeMethod(findMethod(factory), factory, this.timeout);
		}

		private Method findMethod(ClientHttpRequestFactory factory) {
			Method method = ReflectionUtils.findMethod(factory.getClass(),
					this.methodName, int.class);
			if (method != null) {
				return method;
			}
			throw new IllegalStateException("Request factory " + factory.getClass()
					+ " does not have a " + this.methodName + "(int) method");
		}

	}

	/**
	 * {@link RequestFactoryCustomizer} to set the read timeout.
	 */
	private static class ReadTimeoutRequestFactoryCustomizer
			extends TimeoutRequestFactoryCustomizer {

		ReadTimeoutRequestFactoryCustomizer(int readTimeout) {
			super(readTimeout, "setReadTimeout");
		}

	}

	/**
	 * {@link RequestFactoryCustomizer} to set the connect timeout.
	 */
	private static class ConnectTimeoutRequestFactoryCustomizer
			extends TimeoutRequestFactoryCustomizer {

		ConnectTimeoutRequestFactoryCustomizer(int connectTimeout) {
			super(connectTimeout, "setConnectTimeout");
		}

	}

}
