/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Builder that can be used to configure and create a {@link RestTemplate}. Provides
 * convenience methods to register {@link #messageConverters(HttpMessageConverter...)
 * converters}, {@link #errorHandler(ResponseErrorHandler) error handlers} and
 * {@link #uriTemplateHandler(UriTemplateHandler) UriTemplateHandlers}.
 * <p>
 * By default, the built {@link RestTemplate} will attempt to use the most suitable
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
 * @author Scott Frederick
 * @author Yanming Zhou
 * @since 1.4.0
 */
public class RestTemplateBuilder {

	private final ClientHttpRequestFactorySettings requestFactorySettings;

	private final boolean detectRequestFactory;

	private final String rootUri;

	private final Set<HttpMessageConverter<?>> messageConverters;

	private final Set<ClientHttpRequestInterceptor> interceptors;

	private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

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
		Assert.notNull(customizers, "'customizers' must not be null");
		this.requestFactorySettings = ClientHttpRequestFactorySettings.defaults();
		this.detectRequestFactory = true;
		this.rootUri = null;
		this.messageConverters = null;
		this.interceptors = Collections.emptySet();
		this.requestFactoryBuilder = null;
		this.uriTemplateHandler = null;
		this.errorHandler = null;
		this.basicAuthentication = null;
		this.defaultHeaders = Collections.emptyMap();
		this.customizers = copiedSetOf(customizers);
		this.requestCustomizers = Collections.emptySet();
	}

	private RestTemplateBuilder(ClientHttpRequestFactorySettings requestFactorySettings, boolean detectRequestFactory,
			String rootUri, Set<HttpMessageConverter<?>> messageConverters,
			Set<ClientHttpRequestInterceptor> interceptors, ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
			UriTemplateHandler uriTemplateHandler, ResponseErrorHandler errorHandler,
			BasicAuthentication basicAuthentication, Map<String, List<String>> defaultHeaders,
			Set<RestTemplateCustomizer> customizers, Set<RestTemplateRequestCustomizer<?>> requestCustomizers) {
		this.requestFactorySettings = requestFactorySettings;
		this.detectRequestFactory = detectRequestFactory;
		this.rootUri = rootUri;
		this.messageConverters = messageConverters;
		this.interceptors = interceptors;
		this.requestFactoryBuilder = requestFactoryBuilder;
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
		return new RestTemplateBuilder(this.requestFactorySettings, detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Set a root URL that should be applied to each request that starts with {@code '/'}.
	 * The root URL will only apply when {@code String} variants of the
	 * {@link RestTemplate} methods are used for specifying the request URL.
	 * @param rootUri the root URI or {@code null}
	 * @return a new builder instance
	 */
	public RestTemplateBuilder rootUri(String rootUri) {
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
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
		Assert.notNull(messageConverters, "'messageConverters' must not be null");
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
		Assert.notNull(messageConverters, "'messageConverters' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				copiedSetOf(messageConverters), this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
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
		Assert.notNull(messageConverters, "'messageConverters' must not be null");
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
		Assert.notNull(messageConverters, "'messageConverters' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				append(this.messageConverters, messageConverters), this.interceptors, this.requestFactoryBuilder,
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
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				copiedSetOf(new RestTemplate().getMessageConverters()), this.interceptors, this.requestFactoryBuilder,
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
		Assert.notNull(interceptors, "'interceptors' must not be null");
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
		Assert.notNull(interceptors, "'interceptors' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, copiedSetOf(interceptors), this.requestFactoryBuilder, this.uriTemplateHandler,
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
		Assert.notNull(interceptors, "'interceptors' must not be null");
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
		Assert.notNull(interceptors, "'interceptors' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, append(this.interceptors, interceptors), this.requestFactoryBuilder,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Set the {@link ClientHttpRequestFactory} class that should be used with the
	 * {@link RestTemplate}.
	 * @param requestFactoryType the request factory type to use
	 * @return a new builder instance
	 * @see ClientHttpRequestFactoryBuilder#of(Class)
	 * @see #requestFactoryBuilder(ClientHttpRequestFactoryBuilder)
	 */
	public RestTemplateBuilder requestFactory(Class<? extends ClientHttpRequestFactory> requestFactoryType) {
		Assert.notNull(requestFactoryType, "'requestFactoryType' must not be null");
		return requestFactoryBuilder(ClientHttpRequestFactoryBuilder.of(requestFactoryType));
	}

	/**
	 * Set the {@code Supplier} of {@link ClientHttpRequestFactory} that should be called
	 * each time we {@link #build()} a new {@link RestTemplate} instance.
	 * @param requestFactorySupplier the supplier for the request factory
	 * @return a new builder instance
	 * @since 2.0.0
	 * @see ClientHttpRequestFactoryBuilder#of(Supplier)
	 * @see #requestFactoryBuilder(ClientHttpRequestFactoryBuilder)
	 */
	public RestTemplateBuilder requestFactory(Supplier<ClientHttpRequestFactory> requestFactorySupplier) {
		Assert.notNull(requestFactorySupplier, "'requestFactorySupplier' must not be null");
		return requestFactoryBuilder(ClientHttpRequestFactoryBuilder.of(requestFactorySupplier));
	}

	/**
	 * Set the request factory function that should be called to provide a
	 * {@link ClientHttpRequestFactory} each time we {@link #build()} a new
	 * {@link RestTemplate} instance.
	 * @param requestFactoryFunction the settings to request factory function
	 * @return a new builder instance
	 * @since 3.0.0
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #requestFactoryBuilder(ClientHttpRequestFactoryBuilder)}
	 * @see ClientHttpRequestFactoryBuilder
	 * @see #requestFactoryBuilder(ClientHttpRequestFactoryBuilder)
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	@SuppressWarnings("removal")
	public RestTemplateBuilder requestFactory(
			Function<org.springframework.boot.web.client.ClientHttpRequestFactorySettings, ClientHttpRequestFactory> requestFactoryFunction) {
		Assert.notNull(requestFactoryFunction, "'requestFactoryFunction' must not be null");
		return requestFactoryBuilder((settings) -> requestFactoryFunction
			.apply(org.springframework.boot.web.client.ClientHttpRequestFactorySettings.of(settings)));
	}

	/**
	 * Set the {@link ClientHttpRequestFactoryBuilder} that should be used each time we
	 * {@link #build()} a new {@link RestTemplate} instance.
	 * @param requestFactoryBuilder the {@link ClientHttpRequestFactoryBuilder} to use
	 * @return a new builder instance
	 * @since 3.4.0
	 * @see ClientHttpRequestFactoryBuilder
	 */
	public RestTemplateBuilder requestFactoryBuilder(ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder) {
		Assert.notNull(requestFactoryBuilder, "'requestFactoryBuilder' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, requestFactoryBuilder, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Set the {@link UriTemplateHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param uriTemplateHandler the URI template handler to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder uriTemplateHandler(UriTemplateHandler uriTemplateHandler) {
		Assert.notNull(uriTemplateHandler, "'uriTemplateHandler' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Set the {@link ResponseErrorHandler} that should be used with the
	 * {@link RestTemplate}.
	 * @param errorHandler the error handler to use
	 * @return a new builder instance
	 */
	public RestTemplateBuilder errorHandler(ResponseErrorHandler errorHandler) {
		Assert.notNull(errorHandler, "'errorHandler' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
				errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers, this.requestCustomizers);
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
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
				this.errorHandler, new BasicAuthentication(username, password, charset), this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Add a default header that will be set if not already present on the outgoing
	 * {@link ClientHttpRequest}.
	 * @param name the name of the header
	 * @param values the header values
	 * @return a new builder instance
	 * @since 2.2.0
	 */
	public RestTemplateBuilder defaultHeader(String name, String... values) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(values, "'values' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, append(this.defaultHeaders, name, values),
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the {@link ClientHttpRequestFactorySettings}. This will replace any previously
	 * set {@link #connectTimeout(Duration) connectTimeout}, {@link #readTimeout(Duration)
	 * readTimeout} and {@link #sslBundle(SslBundle) sslBundle} values.
	 * @param requestFactorySettings the request factory settings
	 * @return a new builder instance
	 * @since 3.4.0
	 */
	public RestTemplateBuilder requestFactorySettings(ClientHttpRequestFactorySettings requestFactorySettings) {
		Assert.notNull(requestFactorySettings, "'requestFactorySettings' must not be null");
		return new RestTemplateBuilder(requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
				this.errorHandler, this.basicAuthentication, this.defaultHeaders, this.customizers,
				this.requestCustomizers);
	}

	/**
	 * Update the {@link ClientHttpRequestFactorySettings} using the given customizer.
	 * @param requestFactorySettingsCustomizer a {@link UnaryOperator} to update request
	 * factory settings
	 * @return a new builder instance
	 * @since 3.4.1
	 */
	public RestTemplateBuilder requestFactorySettings(
			UnaryOperator<ClientHttpRequestFactorySettings> requestFactorySettingsCustomizer) {
		Assert.notNull(requestFactorySettingsCustomizer, "'requestFactorySettingsCustomizer' must not be null");
		return new RestTemplateBuilder(requestFactorySettingsCustomizer.apply(this.requestFactorySettings),
				this.detectRequestFactory, this.rootUri, this.messageConverters, this.interceptors,
				this.requestFactoryBuilder, this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.defaultHeaders, this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the connection timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param connectTimeout the connection timeout
	 * @return a new builder instance.
	 * @since 2.1.0
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #connectTimeout(Duration)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public RestTemplateBuilder setConnectTimeout(Duration connectTimeout) {
		return connectTimeout(connectTimeout);
	}

	/**
	 * Sets the connection timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param connectTimeout the connection timeout
	 * @return a new builder instance.
	 * @since 3.4.0
	 */
	public RestTemplateBuilder connectTimeout(Duration connectTimeout) {
		return new RestTemplateBuilder(this.requestFactorySettings.withConnectTimeout(connectTimeout),
				this.detectRequestFactory, this.rootUri, this.messageConverters, this.interceptors,
				this.requestFactoryBuilder, this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.defaultHeaders, this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the read timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param readTimeout the read timeout
	 * @return a new builder instance.
	 * @since 2.1.0
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #readTimeout(Duration)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public RestTemplateBuilder setReadTimeout(Duration readTimeout) {
		return readTimeout(readTimeout);
	}

	/**
	 * Sets the read timeout on the underlying {@link ClientHttpRequestFactory}.
	 * @param readTimeout the read timeout
	 * @return a new builder instance.
	 * @since 3.4.0
	 */
	public RestTemplateBuilder readTimeout(Duration readTimeout) {
		return new RestTemplateBuilder(this.requestFactorySettings.withReadTimeout(readTimeout),
				this.detectRequestFactory, this.rootUri, this.messageConverters, this.interceptors,
				this.requestFactoryBuilder, this.uriTemplateHandler, this.errorHandler, this.basicAuthentication,
				this.defaultHeaders, this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the redirect strategy on the underlying {@link ClientHttpRequestFactory}.
	 * @param redirects the redirect strategy
	 * @return a new builder instance.
	 * @since 3.4.1
	 */
	public RestTemplateBuilder redirects(Redirects redirects) {
		return new RestTemplateBuilder(this.requestFactorySettings.withRedirects(redirects), this.detectRequestFactory,
				this.rootUri, this.messageConverters, this.interceptors, this.requestFactoryBuilder,
				this.uriTemplateHandler, this.errorHandler, this.basicAuthentication, this.defaultHeaders,
				this.customizers, this.requestCustomizers);
	}

	/**
	 * Sets the SSL bundle on the underlying {@link ClientHttpRequestFactory}.
	 * @param sslBundle the SSL bundle
	 * @return a new builder instance
	 * @since 3.1.0
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #sslBundle(SslBundle)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	public RestTemplateBuilder setSslBundle(SslBundle sslBundle) {
		return sslBundle(sslBundle);
	}

	/**
	 * Sets the SSL bundle on the underlying {@link ClientHttpRequestFactory}.
	 * @param sslBundle the SSL bundle
	 * @return a new builder instance
	 * @since 3.4.0
	 */
	public RestTemplateBuilder sslBundle(SslBundle sslBundle) {
		return new RestTemplateBuilder(this.requestFactorySettings.withSslBundle(sslBundle), this.detectRequestFactory,
				this.rootUri, this.messageConverters, this.interceptors, this.requestFactoryBuilder,
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
		Assert.notNull(customizers, "'customizers' must not be null");
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
		Assert.notNull(customizers, "'customizers' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
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
		Assert.notNull(customizers, "'customizers' must not be null");
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
		Assert.notNull(customizers, "'customizers' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
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
		Assert.notNull(requestCustomizers, "'requestCustomizers' must not be null");
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
		Assert.notNull(requestCustomizers, "'requestCustomizers' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
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
		Assert.notNull(requestCustomizers, "'requestCustomizers' must not be null");
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
		Assert.notNull(requestCustomizers, "'requestCustomizers' must not be null");
		return new RestTemplateBuilder(this.requestFactorySettings, this.detectRequestFactory, this.rootUri,
				this.messageConverters, this.interceptors, this.requestFactoryBuilder, this.uriTemplateHandler,
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
		return configure(new RestTemplate());
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
			RootUriBuilderFactory.applyTo(restTemplate, this.rootUri);
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
		if (this.requestFactoryBuilder != null) {
			return this.requestFactoryBuilder.build(this.requestFactorySettings);
		}
		if (this.detectRequestFactory) {
			return ClientHttpRequestFactoryBuilder.detect().build(this.requestFactorySettings);
		}
		return null;
	}

	private void addClientHttpRequestInitializer(RestTemplate restTemplate) {
		if (this.basicAuthentication == null && this.defaultHeaders.isEmpty() && this.requestCustomizers.isEmpty()) {
			return;
		}
		restTemplate.getClientHttpRequestInitializers()
			.add(new RestTemplateBuilderClientHttpRequestInitializer(this.basicAuthentication, this.defaultHeaders,
					this.requestCustomizers));
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

}
