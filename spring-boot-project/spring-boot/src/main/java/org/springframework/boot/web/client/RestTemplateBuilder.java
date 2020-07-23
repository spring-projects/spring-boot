/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.netty.http.client.HttpClientRequest;

import org.springframework.beans.BeanUtils;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
 * @author Ilya Lukyanovich
 * @since 1.4.0
 */
public class RestTemplateBuilder {

	private final RequestFactoryCustomizer requestFactoryCustomizer;

	private final boolean detectRequestFactory;

	private final String rootUri;

	private final Set<HttpMessageConverter<?>> messageConverters;

	private final Set<ClientHttpRequestInterceptor> interceptors;

	private final Supplier<ClientHttpRequestFactory> requestFactory;

	private final UriTemplateHandler uriTemplateHandler;

	private final ResponseErrorHandler errorHandler;

	private final BasicAuthentication basicAuthentication;

	private final Map<String, List<String>> defaultHeaders;

	private final Set<RestTemplateCustomizer> customizers;

	private final Set<RestTemplateRequestCustomizer<?>> requestCustomizers;

	/**
	 * Create a new {@link RestTemplateBuilder} instance.
	 * @param customizers any {@link RestTemplateCustomizer RestTemplateCustomizers} that
	 * should be applied when the {@link RestTemplate} is built
	 */
	public RestTemplateBuilder(RestTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		this.requestFactoryCustomizer = new RequestFactoryCustomizer();
		this.detectRequestFactory = true;
		this.rootUri = null;
		this.messageConverters = null;
		this.interceptors = Collections.emptySet();
		this.requestFactory = null;
		this.uriTemplateHandler = null;
		this.errorHandler = null;
		this.basicAuthentication = null;
		this.defaultHeaders = Collections.emptyMap();
		this.customizers = copiedSetOf(customizers);
		this.requestCustomizers = Collections.emptySet();
	}

	private RestTemplateBuilder(RequestFactoryCustomizer requestFactoryCustomizer, boolean detectRequestFactory,
			String rootUri, Set<HttpMessageConverter<?>> messageConverters,
			Set<ClientHttpRequestInterceptor> interceptors, Supplier<ClientHttpRequestFactory> requestFactorySupplier,
			UriTemplateHandler uriTemplateHandler, ResponseErrorHandler errorHandler,
			BasicAuthentication basicAuthentication, Map<String, List<String>> defaultHeaders,
			Set<RestTemplateCustomizer> customizers, Set<RestTemplateRequestCustomizer<?>> requestCustomizers) {
		this.requestFactoryCustomizer = requestFactoryCustomizer;
		this.detectRequestFactory = detectRequestFactory;
		this.rootUri = rootUri;
		this.messageConverters = messageConverters;
		this.interceptors = interceptors;
		this.requestFactory = requestFactorySupplier;
		this.uriTemplateHandler = uriTemplateHandler;
		this.errorHandler = errorHandler;
		this.basicAuthentication = basicAuthentication;
		this.defaultHeaders = defaultHeaders;
		this.customizers = customizers;
		this.requestCustomizers = requestCustomizers;
	}

	/**
	 * Set if the {@link ClientHttpRequestFactory} should be detected based on the
	 * classpath. Default if {@code true}.
	 * @param detectRequestFactory if the {@link ClientHttpRequestFactory} should be
	 * detected
	 * @return a new builder instance
	 */
	public RestTemplateBuilder detectRequestFactory(boolean detectRequestFactory) {
		return new RestTemplateBuilder(this.requestFactoryCustomizer, detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Set a root URL that should be applied to each request that starts with {@code '/'}.
	 * Since this works by adding a {@link UriTemplateHandler} to the
	 * {@link RestTemplate}, the root URL will only apply when {@code String} variants of
	 * the {@link RestTemplate} methods are used for specifying the request URL. See
	 * {@link RootUriTemplateHandler} for details.
	 * @param rootUri the root URI or {@code null}
	 * @return a new builder instance
	 */
	public RestTemplateBuilder rootUri(String rootUri) {
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
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
	public RestTemplateBuilder messageConverters(HttpMessageConverter<?>... messageConverters) {
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
	public RestTemplateBuilder messageConverters(Collection<? extends HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				copiedSetOf(messageConverters), this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
	 * used with the {@link RestTemplate}. Any converters configured on the builder will
	 * replace RestTemplate's default converters.
	 * @param messageConverters the converters to add
	 * @return a new builder instance
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder additionalMessageConverters(HttpMessageConverter<?>... messageConverters) {
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
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				append(this.messageConverters, messageConverters), this.interceptors, this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
	 * the {@link RestTemplate} to the default set. Calling this method will replace any
	 * previously defined converters.
	 * @return a new builder instance
	 * @see #messageConverters(HttpMessageConverter...)
	 */
	public RestTemplateBuilder defaultMessageConverters() {
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				copiedSetOf(new RestTemplate().getMessageConverters()), this.interceptors, this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
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
	public RestTemplateBuilder interceptors(ClientHttpRequestInterceptor... interceptors) {
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
	public RestTemplateBuilder interceptors(Collection<ClientHttpRequestInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, copiedSetOf(interceptors), this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Add additional {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}
	 * that should be used with the {@link RestTemplate}.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @since 1.4.1
	 * @see #interceptors(ClientHttpRequestInterceptor...)
	 */
	public RestTemplateBuilder additionalInterceptors(ClientHttpRequestInterceptor... interceptors) {
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
	public RestTemplateBuilder additionalInterceptors(Collection<? extends ClientHttpRequestInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, append(this.interceptors, interceptors), this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} class that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactory the request factory to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder requestFactory(Class<? extends ClientHttpRequestFactory> requestFactory) {
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		return requestFactory(() -> createRequestFactory(requestFactory));
	}

	private ClientHttpRequestFactory createRequestFactory(Class<? extends ClientHttpRequestFactory> requestFactory) {
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
	 * @param requestFactory the supplier for the request factory
	 * @return a new builder instance
	 * @since 2.0.0
	 */
	public RestTemplateBuilder requestFactory(Supplier<ClientHttpRequestFactory> requestFactory) {
		Assert.notNull(requestFactory, "RequestFactory Supplier must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, requestFactory, this.uriTemplateHandler, this.errorHandler,
				this.basicAuthentication, this.defaultHeaders, this.customizers, this.requestCustomizers);
	}

	/**
	 * Set the {@link UriTemplateHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param uriTemplateHandler the URI template handler to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder uriTemplateHandler(UriTemplateHandler uriTemplateHandler) {
		Assert.notNull(uriTemplateHandler, "UriTemplateHandler must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, uriTemplateHandler, this.errorHandler,
				this.basicAuthentication, this.defaultHeaders, this.customizers, this.requestCustomizers);
	}

	/**
	 * Set the {@link ResponseErrorHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param errorHandler the error handler to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder errorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "ErrorHandler must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler, errorHandler,
				this.basicAuthentication, this.defaultHeaders, this.customizers, this.requestCustomizers);
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
	 */
	public RestTemplateBuilder basicAuthentication(String username, String password, Charset charset) {
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, new BasicAuthentication(username, password, charset), this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Add a default header that will be set if not already present on the outgoing
	 * {@link HttpClientRequest}.
	 * @param name the name of the header
	 * @param values the header values
	 * @return a new builder instance
	 * @since 2.2.0
	 */
	public RestTemplateBuilder defaultHeader(String name, String... values) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(values, "Values must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, append(this.defaultHeaders, name, values),
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the connection timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param connectTimeout the connection timeout
	 * @return a new builder instance.
	 * @since 2.1.0
	 */
	public RestTemplateBuilder setConnectTimeout(Duration connectTimeout) {
		return new RestTemplateBuilder(this.requestFactoryCustomizer.connectTimeout(connectTimeout),
				this.detectRequestFactory, this.rootUri, this.messageConverters, this.interceptors, this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the read timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param readTimeout the read timeout
	 * @return a new builder instance.
	 * @since 2.1.0
	 */
	public RestTemplateBuilder setReadTimeout(Duration readTimeout) {
		return new RestTemplateBuilder(this.requestFactoryCustomizer.readTimeout(readTimeout),
				this.detectRequestFactory, this.rootUri, this.messageConverters, this.interceptors, this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets if the underlying {@link ClientHttpRequestFactory} should buffer the
	 * {@linkplain ClientHttpRequest#getBody() request body} internally.
	 * @param bufferRequestBody value of the bufferRequestBody parameter
	 * @return a new builder instance.
	 * @since 2.2.0
	 * @see SimpleClientHttpRequestFactory#setBufferRequestBody(boolean)
	 * @see HttpComponentsClientHttpRequestFactory#setBufferRequestBody(boolean)
	 */
	public RestTemplateBuilder setBufferRequestBody(boolean bufferRequestBody) {
		return new RestTemplateBuilder(this.requestFactoryCustomizer.bufferRequestBody(bufferRequestBody),
				this.detectRequestFactory, this.rootUri, this.messageConverters, this.interceptors, this.requestFactory,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Set the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
	 * applied to the {@link RestTemplate}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied. Setting this value will
	 * replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder customizers(RestTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
	 * applied to the {@link RestTemplate}. Customizers are applied in the order that they
	 * were added after builder configuration has been applied. Setting this value will
	 * replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder customizers(Collection<? extends RestTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, copiedSetOf(customizers),
				this.requestCustomizers);
	}

	/**
	 * Add {@link RestTemplateCustomizer RestTemplateCustomizers} that should be applied
	 * to the {@link RestTemplate}. Customizers are applied in the order that they were
	 * added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder additionalCustomizers(RestTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add {@link RestTemplateCustomizer RestTemplateCustomizers} that should be applied
	 * to the {@link RestTemplate}. Customizers are applied in the order that they were
	 * added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(RestTemplateCustomizer...)
	 */
	public RestTemplateBuilder additionalCustomizers(Collection<? extends RestTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "RestTemplateCustomizers must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, append(this.customizers, customizers),
				this.requestCustomizers);
	}

	/**
	 * Set the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
	 * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
	 * order that they were added. Setting this value will replace any previously
	 * configured request customizers.
	 * @param requestCustomizers the request customizers to set
	 * @return a new builder instance
	 * @since 2.2.0
	 * @see #additionalRequestCustomizers(RestTemplateRequestCustomizer...)
	 */
	public RestTemplateBuilder requestCustomizers(RestTemplateRequestCustomizer<?>... requestCustomizers) {
		Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
		return requestCustomizers(Arrays.asList(requestCustomizers));
	}

	/**
	 * Set the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
	 * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
	 * order that they were added. Setting this value will replace any previously
	 * configured request customizers.
	 * @param requestCustomizers the request customizers to set
	 * @return a new builder instance
	 * @since 2.2.0
	 * @see #additionalRequestCustomizers(RestTemplateRequestCustomizer...)
	 */
	public RestTemplateBuilder requestCustomizers(
			Collection<? extends RestTemplateRequestCustomizer<?>> requestCustomizers) {
		Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				copiedSetOf(requestCustomizers));
	}

	/**
	 * Add the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
	 * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
	 * order that they were added.
	 * @param requestCustomizers the request customizers to add
	 * @return a new builder instance
	 * @since 2.2.0
	 * @see #requestCustomizers(RestTemplateRequestCustomizer...)
	 */
	public RestTemplateBuilder additionalRequestCustomizers(RestTemplateRequestCustomizer<?>... requestCustomizers) {
		Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
		return additionalRequestCustomizers(Arrays.asList(requestCustomizers));
	}

	/**
	 * Add the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
	 * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
	 * order that they were added.
	 * @param requestCustomizers the request customizers to add
	 * @return a new builder instance
	 * @since 2.2.0
	 * @see #requestCustomizers(Collection)
	 */
	public RestTemplateBuilder additionalRequestCustomizers(
			Collection<? extends RestTemplateRequestCustomizer<?>> requestCustomizers) {
		Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
		return new RestTemplateBuilder(this.requestFactoryCustomizer, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactory, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				append(this.requestCustomizers, requestCustomizers));
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
		addClientHttpRequestInitializer(restTemplate);
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
		if (!CollectionUtils.isEmpty(this.customizers)) {
			for (RestTemplateCustomizer customizer : this.customizers) {
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
		if (this.requestFactory != null) {
			requestFactory = this.requestFactory.get();
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

	private void addClientHttpRequestInitializer(RestTemplate restTemplate) {
		if (this.basicAuthentication == null && this.defaultHeaders.isEmpty() && this.requestCustomizers.isEmpty()) {
			return;
		}
		restTemplate.getClientHttpRequestInitializers().add(new RestTemplateBuilderClientHttpRequestInitializer(
				this.basicAuthentication, this.defaultHeaders, this.requestCustomizers));
	}

	@SuppressWarnings("unchecked")
	private <T> Set<T> copiedSetOf(T... items) {
		return copiedSetOf(Arrays.asList(items));
	}

	private <T> Set<T> copiedSetOf(Collection<? extends T> collection) {
		return Collections.unmodifiableSet(new LinkedHashSet<>(collection));
	}

	private static <T> List<T> copiedListOf(T[] items) {
		return Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(items, items.length)));
	}

	private static <T> Set<T> append(Collection<? extends T> collection, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>((collection != null) ? collection : Collections.emptySet());
		if (additions != null) {
			result.addAll(additions);
		}
		return Collections.unmodifiableSet(result);
	}

	private static <K, V> Map<K, List<V>> append(Map<K, List<V>> map, K key, V[] values) {
		Map<K, List<V>> result = new LinkedHashMap<>((map != null) ? map : Collections.emptyMap());
		if (values != null) {
			result.put(key, copiedListOf(values));
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * Internal customizer used to apply {@link ClientHttpRequestFactory} settings.
	 */
	private static class RequestFactoryCustomizer implements Consumer<ClientHttpRequestFactory> {

		private final Duration connectTimeout;

		private final Duration readTimeout;

		private final Boolean bufferRequestBody;

		RequestFactoryCustomizer() {
			this(null, null, null);
		}

		private RequestFactoryCustomizer(Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody) {
			this.connectTimeout = connectTimeout;
			this.readTimeout = readTimeout;
			this.bufferRequestBody = bufferRequestBody;
		}

		RequestFactoryCustomizer connectTimeout(Duration connectTimeout) {
			return new RequestFactoryCustomizer(connectTimeout, this.readTimeout, this.bufferRequestBody);
		}

		RequestFactoryCustomizer readTimeout(Duration readTimeout) {
			return new RequestFactoryCustomizer(this.connectTimeout, readTimeout, this.bufferRequestBody);
		}

		RequestFactoryCustomizer bufferRequestBody(boolean bufferRequestBody) {
			return new RequestFactoryCustomizer(this.connectTimeout, this.readTimeout, bufferRequestBody);
		}

		@Override
		public void accept(ClientHttpRequestFactory requestFactory) {
			ClientHttpRequestFactory unwrappedRequestFactory = unwrapRequestFactoryIfNecessary(requestFactory);
			if (this.connectTimeout != null) {
				setConnectTimeout(unwrappedRequestFactory);
			}
			if (this.readTimeout != null) {
				setReadTimeout(unwrappedRequestFactory);
			}
			if (this.bufferRequestBody != null) {
				setBufferRequestBody(unwrappedRequestFactory);
			}
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

		private void setConnectTimeout(ClientHttpRequestFactory factory) {
			Method method = findMethod(factory, "setConnectTimeout", int.class);
			int timeout = Math.toIntExact(this.connectTimeout.toMillis());
			invoke(factory, method, timeout);
		}

		private void setReadTimeout(ClientHttpRequestFactory factory) {
			Method method = findMethod(factory, "setReadTimeout", int.class);
			int timeout = Math.toIntExact(this.readTimeout.toMillis());
			invoke(factory, method, timeout);
		}

		private void setBufferRequestBody(ClientHttpRequestFactory factory) {
			Method method = findMethod(factory, "setBufferRequestBody", boolean.class);
			invoke(factory, method, this.bufferRequestBody);
		}

		private Method findMethod(ClientHttpRequestFactory requestFactory, String methodName, Class<?>... parameters) {
			Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
			if (method != null) {
				return method;
			}
			throw new IllegalStateException("Request factory " + requestFactory.getClass()
					+ " does not have a suitable " + methodName + " method");
		}

		private void invoke(ClientHttpRequestFactory requestFactory, Method method, Object... parameters) {
			ReflectionUtils.invokeMethod(method, requestFactory, parameters);
		}

	}

}
