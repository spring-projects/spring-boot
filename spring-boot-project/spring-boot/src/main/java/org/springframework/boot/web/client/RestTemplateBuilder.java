/*
 * Copyright 2012-2019 the original author or authors.
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
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
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
 * @author Brian Clozel
 * @author Dmytro Nosan
 * @author Kevin Strijbos
 * @since 1.4.0
 */
public class RestTemplateBuilder {

	private final boolean detectRequestFactory;

	private final String rootUri;

	private final Set<HttpMessageConverter<?>> messageConverters;

	private final Supplier<ClientHttpRequestFactory> requestFactorySupplier;

	private final UriTemplateHandler uriTemplateHandler;

	private final ResponseErrorHandler errorHandler;

	private final BasicAuthentication basicAuthentication;

	private final Set<RestTemplateCustomizer> restTemplateCustomizers;

	private final RequestFactoryCustomizer requestFactoryCustomizer;

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
		this.requestFactorySupplier = null;
		this.uriTemplateHandler = null;
		this.errorHandler = null;
		this.basicAuthentication = null;
		this.restTemplateCustomizers = Collections
				.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(customizers)));
		this.requestFactoryCustomizer = new RequestFactoryCustomizer();
		this.interceptors = Collections.emptySet();
	}

	private RestTemplateBuilder(boolean detectRequestFactory, String rootUri,
			Set<HttpMessageConverter<?>> messageConverters,
			Supplier<ClientHttpRequestFactory> requestFactorySupplier,
			UriTemplateHandler uriTemplateHandler, ResponseErrorHandler errorHandler,
			BasicAuthentication basicAuthentication,
			Set<RestTemplateCustomizer> restTemplateCustomizers,
			RequestFactoryCustomizer requestFactoryCustomizer,
			Set<ClientHttpRequestInterceptor> interceptors) {
		this.detectRequestFactory = detectRequestFactory;
		this.rootUri = rootUri;
		this.messageConverters = messageConverters;
		this.requestFactorySupplier = requestFactorySupplier;
		this.uriTemplateHandler = uriTemplateHandler;
		this.errorHandler = errorHandler;
		this.basicAuthentication = basicAuthentication;
		this.restTemplateCustomizers = restTemplateCustomizers;
		this.requestFactoryCustomizer = requestFactoryCustomizer;
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
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers, this.requestFactoryCustomizer,
				this.interceptors);
	}

	/**
	 * Set a root URL that should be applied to each request that starts with {@code '/'}.
	 * See {@link RootUriTemplateHandler} for details.
	 * @param rootUri the root URI or {@code null}
	 * @return a new builder instance
	 */
	public RestTemplateBuilder rootUri(String rootUri) {
		return new RestTemplateBuilder(this.detectRequestFactory, rootUri,
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers, this.requestFactoryCustomizer,
				this.interceptors);
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate}. Setting this value will replace any previously configured
	 * converters and any converters configured on the builder will replace RestTemplate's
	 * default converters.
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
	 * converters and any converters configured on the builder will replace RestTemplate's
	 * default converters.
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
				this.requestFactorySupplier, this.uriTemplateHandler, this.errorHandler,
				this.basicAuthentication, this.restTemplateCustomizers,
				this.requestFactoryCustomizer, this.interceptors);
	}

	/**
	 * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
	 * used with the {@link RestTemplate}. Any converters configured on the builder will
	 * replace RestTemplate's default converters.
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
	 * used with the {@link RestTemplate}. Any converters configured on the builder will
	 * replace RestTemplate's default converters.
	 * @param messageConverters the converters to add
	 * @return a new builder instance
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder additionalMessageConverters(
			Collection<? extends HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				append(this.messageConverters, messageConverters),
				this.requestFactorySupplier, this.uriTemplateHandler, this.errorHandler,
				this.basicAuthentication, this.restTemplateCustomizers,
				this.requestFactoryCustomizer, this.interceptors);
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
				this.requestFactorySupplier, this.uriTemplateHandler, this.errorHandler,
				this.basicAuthentication, this.restTemplateCustomizers,
				this.requestFactoryCustomizer, this.interceptors);
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
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers, this.requestFactoryCustomizer,
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
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers, this.requestFactoryCustomizer,
				append(this.interceptors, interceptors));
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
		return requestFactory(() -> createRequestFactory(requestFactory));
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
	 * Set the {@code Supplier} of {@link ClientHttpRequestFactory} that should be called
	 * each time we {@link #build()} a new {@link RestTemplate} instance.
	 * @param requestFactorySupplier the supplier for the request factory
	 * @return a new builder instance
	 * @since 2.0.0
	 */
	public RestTemplateBuilder requestFactory(
			Supplier<ClientHttpRequestFactory> requestFactorySupplier) {
		Assert.notNull(requestFactorySupplier,
				"RequestFactory Supplier must not be null");
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, requestFactorySupplier, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.restTemplateCustomizers,
				this.requestFactoryCustomizer, this.interceptors);
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
				this.messageConverters, this.requestFactorySupplier, uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.restTemplateCustomizers,
				this.requestFactoryCustomizer, this.interceptors);
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
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers, this.requestFactoryCustomizer,
				this.interceptors);
	}

	/**
	 * Add HTTP Basic Authentication to requests with the given username/password pair,
	 * unless a custom Authorization header has been set before.
	 * @param username the user name
	 * @param password the password
	 * @return a new builder instance
	 * @since 2.1.0
	 * @see #basicAuthentication(String, String, Charset)
	 */
	public RestTemplateBuilder basicAuthentication(String username, String password) {
		return basicAuthentication(username, password, null);
	}

	/**
	 * Add HTTP Basic Authentication to requests with the given username/password pair,
	 * unless a custom Authorization header has been set before.
	 * @param username the user name
	 * @param password the password
	 * @param charset the charset to use
	 * @return a new builder instance
	 * @since 2.2.0
	 * @see #basicAuthentication(String, String)
	 */
	public RestTemplateBuilder basicAuthentication(String username, String password,
			Charset charset) {
		BasicAuthentication basicAuthentication = new BasicAuthentication(username,
				password, charset);
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, basicAuthentication,
				this.restTemplateCustomizers, this.requestFactoryCustomizer,
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
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				Collections.unmodifiableSet(new LinkedHashSet<RestTemplateCustomizer>(
						restTemplateCustomizers)),
				this.requestFactoryCustomizer, this.interceptors);
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
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				append(this.restTemplateCustomizers, customizers),
				this.requestFactoryCustomizer, this.interceptors);
	}

	/**
	 * Sets the connection timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param connectTimeout the connection timeout
	 * @return a new builder instance.
	 * @since 2.1.0
	 */
	public RestTemplateBuilder setConnectTimeout(Duration connectTimeout) {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers,
				this.requestFactoryCustomizer.connectTimeout(connectTimeout),
				this.interceptors);
	}

	/**
	 * Sets the read timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param readTimeout the read timeout
	 * @return a new builder instance.
	 * @since 2.1.0
	 */
	public RestTemplateBuilder setReadTimeout(Duration readTimeout) {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers,
				this.requestFactoryCustomizer.readTimeout(readTimeout),
				this.interceptors);
	}

	/**
	 * Sets the bufferrequestbody value on the underlying
	 * {@link ClientHttpRequestFactory}.
	 * @param bufferRequestBody value of the bufferRequestBody parameter
	 * @return a new builder instance.
	 * @since 2.1.0
	 */
	public RestTemplateBuilder setBufferRequestBody(boolean bufferRequestBody) {
		return new RestTemplateBuilder(this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.requestFactorySupplier,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.restTemplateCustomizers,
				this.requestFactoryCustomizer.bufferRequestBody(bufferRequestBody),
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
		ClientHttpRequestFactory requestFactory = buildRequestFactory();
		if (requestFactory != null) {
			restTemplate.setRequestFactory(requestFactory);
		}
		if (this.basicAuthentication != null) {
			configureBasicAuthentication(restTemplate);
		}
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
		restTemplate.getInterceptors().addAll(this.interceptors);
		if (!CollectionUtils.isEmpty(this.restTemplateCustomizers)) {
			for (RestTemplateCustomizer customizer : this.restTemplateCustomizers) {
				customizer.customize(restTemplate);
			}
		}
		return restTemplate;
	}

	/**
	 * Build a new {@link ClientHttpRequestFactory} instance using the settings of this
	 * builder.
	 * @return a {@link ClientHttpRequestFactory} or {@code null}
	 * @since 2.2.0
	 */
	public ClientHttpRequestFactory buildRequestFactory() {
		ClientHttpRequestFactory requestFactory = null;
		if (this.requestFactorySupplier != null) {
			requestFactory = this.requestFactorySupplier.get();
		}
		else if (this.detectRequestFactory) {
			requestFactory = new ClientHttpRequestFactorySupplier().get();
		}
		if (requestFactory != null) {
			if (this.requestFactoryCustomizer != null) {
				this.requestFactoryCustomizer.accept(requestFactory);
			}
		}
		return requestFactory;
	}

	private void configureBasicAuthentication(RestTemplate restTemplate) {
		List<ClientHttpRequestInterceptor> interceptors = null;
		if (!restTemplate.getInterceptors().isEmpty()) {
			// Stash and clear the interceptors so we can access the real factory
			interceptors = new ArrayList<>(restTemplate.getInterceptors());
			restTemplate.getInterceptors().clear();
		}
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		restTemplate.setRequestFactory(new BasicAuthenticationClientHttpRequestFactory(
				this.basicAuthentication, requestFactory));
		// Restore the original interceptors
		if (interceptors != null) {
			restTemplate.getInterceptors().addAll(interceptors);
		}
	}

	private <T> Set<T> append(Set<T> set, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
		result.addAll(additions);
		return Collections.unmodifiableSet(result);
	}

	private static class RequestFactoryCustomizer
			implements Consumer<ClientHttpRequestFactory> {

		private final Duration connectTimeout;

		private final Duration readTimeout;

		private final boolean bufferRequestBody;

		private final boolean bufferRequestBodyFlag;

		RequestFactoryCustomizer() {
			this(null, null, true, false);
		}

		private RequestFactoryCustomizer(Duration connectTimeout, Duration readTimeout,
				boolean bufferRequestBody, boolean bufferRequestBodyFlag) {
			this.connectTimeout = connectTimeout;
			this.readTimeout = readTimeout;
			this.bufferRequestBody = bufferRequestBody;
			this.bufferRequestBodyFlag = bufferRequestBodyFlag;
		}

		public RequestFactoryCustomizer connectTimeout(Duration connectTimeout) {
			return new RequestFactoryCustomizer(connectTimeout, this.readTimeout,
					this.bufferRequestBody, this.bufferRequestBodyFlag);
		}

		public RequestFactoryCustomizer readTimeout(Duration readTimeout) {
			return new RequestFactoryCustomizer(this.connectTimeout, readTimeout,
					this.bufferRequestBody, this.bufferRequestBodyFlag);
		}

		public RequestFactoryCustomizer bufferRequestBody(boolean bufferRequestBody) {
			return new RequestFactoryCustomizer(this.connectTimeout, this.readTimeout,
					bufferRequestBody, true);
		}

		@Override
		public void accept(ClientHttpRequestFactory requestFactory) {
			ClientHttpRequestFactory unwrappedRequestFactory = unwrapRequestFactoryIfNecessary(
					requestFactory);
			if (this.connectTimeout != null) {
				new TimeoutRequestFactoryCustomizer(this.connectTimeout,
						"setConnectTimeout").customize(unwrappedRequestFactory);
			}
			if (this.readTimeout != null) {
				new TimeoutRequestFactoryCustomizer(this.readTimeout, "setReadTimeout")
						.customize(unwrappedRequestFactory);
			}
			if (this.bufferRequestBodyFlag) {
				new BufferRequestBodyFactoryCustomizer(this.bufferRequestBody,
						"setBufferRequestBody").customize(unwrappedRequestFactory);
			}
		}

		private ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(
				ClientHttpRequestFactory requestFactory) {
			if (!(requestFactory instanceof AbstractClientHttpRequestFactoryWrapper)) {
				return requestFactory;
			}
			Field field = ReflectionUtils.findField(
					AbstractClientHttpRequestFactoryWrapper.class, "requestFactory");
			ReflectionUtils.makeAccessible(field);
			ClientHttpRequestFactory unwrappedRequestFactory = requestFactory;
			while (unwrappedRequestFactory instanceof AbstractClientHttpRequestFactoryWrapper) {
				unwrappedRequestFactory = (ClientHttpRequestFactory) ReflectionUtils
						.getField(field, unwrappedRequestFactory);
			}
			return unwrappedRequestFactory;
		}

		/**
		 * {@link ClientHttpRequestFactory} customizer to call a "set timeout" method.
		 */
		private static final class TimeoutRequestFactoryCustomizer {

			private final Duration timeout;

			private final String methodName;

			TimeoutRequestFactoryCustomizer(Duration timeout, String methodName) {
				this.timeout = timeout;
				this.methodName = methodName;
			}

			void customize(ClientHttpRequestFactory factory) {
				ReflectionUtils.invokeMethod(findMethod(factory), factory,
						Math.toIntExact(this.timeout.toMillis()));
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
		 * {@link ClientHttpRequestFactory} customizer to call a "set buffer request body"
		 * method.
		 */
		private static final class BufferRequestBodyFactoryCustomizer {

			private final boolean bufferRequestBody;

			private final String methodName;

			BufferRequestBodyFactoryCustomizer(boolean bufferRequestBody,
					String methodName) {
				this.bufferRequestBody = bufferRequestBody;
				this.methodName = methodName;
			}

			void customize(ClientHttpRequestFactory factory) {
				ReflectionUtils.invokeMethod(findMethod(factory), factory,
						this.bufferRequestBody);
			}

			private Method findMethod(ClientHttpRequestFactory factory) {
				Method method = ReflectionUtils.findMethod(factory.getClass(),
						this.methodName, boolean.class);
				if (method != null) {
					return method;
				}
				throw new IllegalStateException("Request factory " + factory.getClass()
						+ " does not have a " + this.methodName + "(boolean) method");
			}

		}

	}

}
