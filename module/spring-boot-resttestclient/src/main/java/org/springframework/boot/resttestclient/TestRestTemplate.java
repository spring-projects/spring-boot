/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.resttestclient;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpComponentsHttpClientBuilder.TlsSocketStrategyFactory;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.RootUriTemplateHandler;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.RequestEntity.UriTemplateRequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.NoOpResponseErrorHandler;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Convenient alternative of {@link RestTemplate} that is suitable for integration tests.
 * {@code TestRestTemplate} is fault-tolerant. This means that 4xx and 5xx do not result
 * in an exception being thrown and can instead be detected through the
 * {@link ResponseEntity response entity} and its {@link ResponseEntity#getStatusCode()
 * status code}.
 * <p>
 * A {@code TestRestTemplate} can optionally carry Basic authentication headers. If Apache
 * Http Client 4.3.2 or better is available (recommended) it will be used as the client,
 * and by default configured to ignore cookies.
 * <p>
 * Note: To prevent injection problems this class intentionally does not extend
 * {@link RestTemplate}. If you need access to the underlying {@link RestTemplate} use
 * {@link #getRestTemplate()}.
 * <p>
 * If you are using the {@code @SpringBootTest} annotation with an embedded server, a
 * {@link TestRestTemplate} can be auto-configured by adding
 * {@link AutoConfigureTestRestTemplate @AutoConfigureTestRestTemplate} to your test
 * class. It can then be {@code @Autowired} into your test. If you need customizations
 * (for example to adding additional message converters) use a {@link RestTemplateBuilder}
 * {@code @Bean}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kristine Jetzke
 * @author Dmytro Nosan
 * @author Yanming Zhou
 * @since 4.0.0
 */
public class TestRestTemplate {

	private final RestTemplateBuilder builder;

	private final RestTemplate restTemplate;

	/**
	 * Create a new {@link TestRestTemplate} instance.
	 * @param restTemplateBuilder builder used to configure underlying
	 * {@link RestTemplate}
	 */
	public TestRestTemplate(RestTemplateBuilder restTemplateBuilder) {
		this(restTemplateBuilder, null, null);
	}

	/**
	 * Create a new {@link TestRestTemplate} instance.
	 * @param httpClientOptions client options to use if the Apache HTTP Client is used
	 */
	public TestRestTemplate(HttpClientOption... httpClientOptions) {
		this(null, null, httpClientOptions);
	}

	/**
	 * Create a new {@link TestRestTemplate} instance with the specified credentials.
	 * @param username the username to use (or {@code null})
	 * @param password the password (or {@code null})
	 * @param httpClientOptions client options to use if the Apache HTTP Client is used
	 */
	public TestRestTemplate(@Nullable String username, @Nullable String password,
			HttpClientOption... httpClientOptions) {
		this(new RestTemplateBuilder(), username, password, httpClientOptions);
	}

	/**
	 * Create a new {@link TestRestTemplate} instance with the specified credentials.
	 * @param builder builder used to configure underlying {@link RestTemplate}
	 * @param username the username to use (or {@code null})
	 * @param password the password (or {@code null})
	 * @param httpClientOptions client options to use if the Apache HTTP Client is used
	 */
	public TestRestTemplate(RestTemplateBuilder builder, @Nullable String username, @Nullable String password,
			HttpClientOption... httpClientOptions) {
		this(createInitialBuilder(builder, username, password, httpClientOptions), null);
	}

	private TestRestTemplate(RestTemplateBuilder builder, @Nullable UriTemplateHandler uriTemplateHandler) {
		this.builder = builder;
		this.restTemplate = builder.build();
		if (uriTemplateHandler != null) {
			this.restTemplate.setUriTemplateHandler(uriTemplateHandler);
		}
		this.restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
	}

	private static RestTemplateBuilder createInitialBuilder(RestTemplateBuilder builder, @Nullable String username,
			@Nullable String password, HttpClientOption... httpClientOptions) {
		Assert.notNull(builder, "'builder' must not be null");
		ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder = builder.requestFactoryBuilder();
		if (requestFactoryBuilder instanceof HttpComponentsClientHttpRequestFactoryBuilder) {
			builder = builder.requestFactoryBuilder(applyHttpClientOptions(
					(HttpComponentsClientHttpRequestFactoryBuilder) requestFactoryBuilder, httpClientOptions));
		}
		if (username != null && password != null) {
			builder = builder.basicAuthentication(username, password);
		}
		return builder;
	}

	private static HttpComponentsClientHttpRequestFactoryBuilder applyHttpClientOptions(
			HttpComponentsClientHttpRequestFactoryBuilder builder, HttpClientOption[] httpClientOptions) {
		builder = builder.withDefaultRequestConfigCustomizer(
				new CookieSpecCustomizer(HttpClientOption.ENABLE_COOKIES.isPresent(httpClientOptions)));
		if (HttpClientOption.SSL.isPresent(httpClientOptions)) {
			builder = builder.withTlsSocketStrategyFactory(new SelfSignedTlsSocketStrategyFactory());
		}
		return builder;
	}

	/**
	 * Configure the {@link UriTemplateHandler} to use to expand URI templates. By default
	 * the {@link DefaultUriBuilderFactory} is used which relies on Spring's URI template
	 * support and exposes several useful properties that customize its behavior for
	 * encoding and for prepending a common base URL. An alternative implementation may be
	 * used to plug an external URI template library.
	 * @param handler the URI template handler to use
	 */
	public void setUriTemplateHandler(UriTemplateHandler handler) {
		this.restTemplate.setUriTemplateHandler(handler);
	}

	/**
	 * Returns the root URI applied by {@link RestTemplateBuilder#rootUri(String)} or
	 * {@code ""} if the root URI has not been applied.
	 * @return the root URI
	 */
	public @Nullable String getRootUri() {
		UriTemplateHandler uriTemplateHandler = this.restTemplate.getUriTemplateHandler();
		if (uriTemplateHandler instanceof RootUriTemplateHandler rootHandler) {
			return rootHandler.getRootUri();
		}
		return "";
	}

	/**
	 * Retrieve a representation by doing a GET on the specified URL. The response (if
	 * any) is converted and returned.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see RestTemplate#getForObject(String, Class, Object...)
	 */
	public <T> @Nullable T getForObject(String url, Class<T> responseType, Object... urlVariables) {
		return this.restTemplate.getForObject(url, responseType, urlVariables);
	}

	/**
	 * Retrieve a representation by doing a GET on the URI template. The response (if any)
	 * is converted and returned.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param urlVariables the map containing variables for the URI template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see RestTemplate#getForObject(String, Class, Object...)
	 */
	public <T> @Nullable T getForObject(String url, Class<T> responseType, Map<String, ?> urlVariables) {
		return this.restTemplate.getForObject(url, responseType, urlVariables);
	}

	/**
	 * Retrieve a representation by doing a GET on the URL . The response (if any) is
	 * converted and returned.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see RestTemplate#getForObject(java.net.URI, java.lang.Class)
	 */
	public <T> @Nullable T getForObject(URI url, Class<T> responseType) {
		return this.restTemplate.getForObject(applyRootUriIfNecessary(url), responseType);
	}

	/**
	 * Retrieve an entity by doing a GET on the specified URL. The response is converted
	 * and stored in an {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the entity
	 * @see RestTemplate#getForEntity(java.lang.String, java.lang.Class,
	 * java.lang.Object[])
	 */
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... urlVariables) {
		return this.restTemplate.getForEntity(url, responseType, urlVariables);
	}

	/**
	 * Retrieve a representation by doing a GET on the URI template. The response is
	 * converted and stored in an {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param urlVariables the map containing variables for the URI template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see RestTemplate#getForEntity(java.lang.String, java.lang.Class, java.util.Map)
	 */
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Map<String, ?> urlVariables) {
		return this.restTemplate.getForEntity(url, responseType, urlVariables);
	}

	/**
	 * Retrieve a representation by doing a GET on the URL . The response is converted and
	 * stored in an {@link ResponseEntity}.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see RestTemplate#getForEntity(java.net.URI, java.lang.Class)
	 */
	public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType) {
		return this.restTemplate.getForEntity(applyRootUriIfNecessary(url), responseType);
	}

	/**
	 * Retrieve all headers of the resource specified by the URI template.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param urlVariables the variables to expand the template
	 * @return all HTTP headers of that resource
	 * @see RestTemplate#headForHeaders(java.lang.String, java.lang.Object[])
	 */
	public HttpHeaders headForHeaders(String url, Object... urlVariables) {
		return this.restTemplate.headForHeaders(url, urlVariables);
	}

	/**
	 * Retrieve all headers of the resource specified by the URI template.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param urlVariables the map containing variables for the URI template
	 * @return all HTTP headers of that resource
	 * @see RestTemplate#headForHeaders(java.lang.String, java.util.Map)
	 */
	public HttpHeaders headForHeaders(String url, Map<String, ?> urlVariables) {
		return this.restTemplate.headForHeaders(url, urlVariables);
	}

	/**
	 * Retrieve all headers of the resource specified by the URL.
	 * @param url the URL
	 * @return all HTTP headers of that resource
	 * @see RestTemplate#headForHeaders(java.net.URI)
	 */
	public HttpHeaders headForHeaders(URI url) {
		return this.restTemplate.headForHeaders(applyRootUriIfNecessary(url));
	}

	/**
	 * Create a new resource by POSTing the given object to the URI template, and returns
	 * the value of the {@code Location} header. This header typically indicates where the
	 * new resource is stored.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param urlVariables the variables to expand the template
	 * @return the value for the {@code Location} header
	 * @see HttpEntity
	 * @see RestTemplate#postForLocation(java.lang.String, java.lang.Object,
	 * java.lang.Object[])
	 */
	public @Nullable URI postForLocation(String url, @Nullable Object request, Object... urlVariables) {
		return this.restTemplate.postForLocation(url, request, urlVariables);
	}

	/**
	 * Create a new resource by POSTing the given object to the URI template, and returns
	 * the value of the {@code Location} header. This header typically indicates where the
	 * new resource is stored.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param urlVariables the variables to expand the template
	 * @return the value for the {@code Location} header
	 * @see HttpEntity
	 * @see RestTemplate#postForLocation(java.lang.String, java.lang.Object,
	 * java.util.Map)
	 */
	public @Nullable URI postForLocation(String url, @Nullable Object request, Map<String, ?> urlVariables) {
		return this.restTemplate.postForLocation(url, request, urlVariables);
	}

	/**
	 * Create a new resource by POSTing the given object to the URL, and returns the value
	 * of the {@code Location} header. This header typically indicates where the new
	 * resource is stored.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @return the value for the {@code Location} header
	 * @see HttpEntity
	 * @see RestTemplate#postForLocation(java.net.URI, java.lang.Object)
	 */
	public @Nullable URI postForLocation(URI url, @Nullable Object request) {
		return this.restTemplate.postForLocation(applyRootUriIfNecessary(url), request);
	}

	/**
	 * Create a new resource by POSTing the given object to the URI template, and returns
	 * the representation found in the response.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 * @see RestTemplate#postForObject(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.lang.Object[])
	 */
	public <T> @Nullable T postForObject(String url, @Nullable Object request, Class<T> responseType,
			Object... urlVariables) {
		return this.restTemplate.postForObject(url, request, responseType, urlVariables);
	}

	/**
	 * Create a new resource by POSTing the given object to the URI template, and returns
	 * the representation found in the response.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 * @see RestTemplate#postForObject(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.util.Map)
	 */
	public <T> @Nullable T postForObject(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> urlVariables) {
		return this.restTemplate.postForObject(url, request, responseType, urlVariables);
	}

	/**
	 * Create a new resource by POSTing the given object to the URL, and returns the
	 * representation found in the response.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 * @see RestTemplate#postForObject(java.net.URI, java.lang.Object, java.lang.Class)
	 */
	public <T> @Nullable T postForObject(URI url, @Nullable Object request, Class<T> responseType) {
		return this.restTemplate.postForObject(applyRootUriIfNecessary(url), request, responseType);
	}

	/**
	 * Create a new resource by POSTing the given object to the URI template, and returns
	 * the response as {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the response type to return
	 * @param urlVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 * @see RestTemplate#postForEntity(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.lang.Object[])
	 */
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
			Object... urlVariables) {
		return this.restTemplate.postForEntity(url, request, responseType, urlVariables);
	}

	/**
	 * Create a new resource by POSTing the given object to the URI template, and returns
	 * the response as {@link HttpEntity}.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the response type to return
	 * @param urlVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 * @see RestTemplate#postForEntity(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.util.Map)
	 */
	public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> urlVariables) {
		return this.restTemplate.postForEntity(url, request, responseType, urlVariables);
	}

	/**
	 * Create a new resource by POSTing the given object to the URL, and returns the
	 * response as {@link ResponseEntity}.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the response type to return
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 * @see RestTemplate#postForEntity(java.net.URI, java.lang.Object, java.lang.Class)
	 */
	public <T> ResponseEntity<T> postForEntity(URI url, @Nullable Object request, Class<T> responseType) {
		return this.restTemplate.postForEntity(applyRootUriIfNecessary(url), request, responseType);
	}

	/**
	 * Create or update a resource by PUTting the given object to the URI.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * <p>
	 * If you need to assert the request result consider using the
	 * {@link TestRestTemplate#exchange exchange} method.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @param urlVariables the variables to expand the template
	 * @see HttpEntity
	 * @see RestTemplate#put(java.lang.String, java.lang.Object, java.lang.Object[])
	 */
	public void put(String url, @Nullable Object request, Object... urlVariables) {
		this.restTemplate.put(url, request, urlVariables);
	}

	/**
	 * Creates a new resource by PUTting the given object to URI template.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * <p>
	 * If you need to assert the request result consider using the
	 * {@link TestRestTemplate#exchange exchange} method.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @param urlVariables the variables to expand the template
	 * @see HttpEntity
	 * @see RestTemplate#put(java.lang.String, java.lang.Object, java.util.Map)
	 */
	public void put(String url, @Nullable Object request, Map<String, ?> urlVariables) {
		this.restTemplate.put(url, request, urlVariables);
	}

	/**
	 * Creates a new resource by PUTting the given object to URL.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * <p>
	 * If you need to assert the request result consider using the
	 * {@link TestRestTemplate#exchange exchange} method.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @see HttpEntity
	 * @see RestTemplate#put(java.net.URI, java.lang.Object)
	 */
	public void put(URI url, @Nullable Object request) {
		this.restTemplate.put(applyRootUriIfNecessary(url), request);
	}

	/**
	 * Update a resource by PATCHing the given object to the URI template, and returns the
	 * representation found in the response.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be PATCHed, may be {@code null}
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 */
	public <T> @Nullable T patchForObject(String url, @Nullable Object request, Class<T> responseType,
			Object... uriVariables) {
		return this.restTemplate.patchForObject(url, request, responseType, uriVariables);
	}

	/**
	 * Update a resource by PATCHing the given object to the URI template, and returns the
	 * representation found in the response.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be PATCHed, may be {@code null}
	 * @param responseType the type of the return value
	 * @param uriVariables the variables to expand the template
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 */
	public <T> @Nullable T patchForObject(String url, @Nullable Object request, Class<T> responseType,
			Map<String, ?> uriVariables) {
		return this.restTemplate.patchForObject(url, request, responseType, uriVariables);
	}

	/**
	 * Update a resource by PATCHing the given object to the URL, and returns the
	 * representation found in the response.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be POSTed, may be {@code null}
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @see HttpEntity
	 */
	public <T> @Nullable T patchForObject(URI url, @Nullable Object request, Class<T> responseType) {
		return this.restTemplate.patchForObject(applyRootUriIfNecessary(url), request, responseType);
	}

	/**
	 * Delete the resources at the specified URI.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * If you need to assert the request result consider using the
	 * {@link TestRestTemplate#exchange exchange} method.
	 * @param url the URL
	 * @param urlVariables the variables to expand in the template
	 * @see RestTemplate#delete(java.lang.String, java.lang.Object[])
	 */
	public void delete(String url, Object... urlVariables) {
		this.restTemplate.delete(url, urlVariables);
	}

	/**
	 * Delete the resources at the specified URI.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * If you need to assert the request result consider using the
	 * {@link TestRestTemplate#exchange exchange} method.
	 * @param url the URL
	 * @param urlVariables the variables to expand the template
	 * @see RestTemplate#delete(java.lang.String, java.util.Map)
	 */
	public void delete(String url, Map<String, ?> urlVariables) {
		this.restTemplate.delete(url, urlVariables);
	}

	/**
	 * Delete the resources at the specified URL.
	 * <p>
	 * If you need to assert the request result consider using the
	 * {@link TestRestTemplate#exchange exchange} method.
	 * @param url the URL
	 * @see RestTemplate#delete(java.net.URI)
	 */
	public void delete(URI url) {
		this.restTemplate.delete(applyRootUriIfNecessary(url));
	}

	/**
	 * Return the value of the {@code Allow} header for the given URI.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param urlVariables the variables to expand in the template
	 * @return the value of the {@code Allow} header
	 * @see RestTemplate#optionsForAllow(java.lang.String, java.lang.Object[])
	 */
	public Set<HttpMethod> optionsForAllow(String url, Object... urlVariables) {
		return this.restTemplate.optionsForAllow(url, urlVariables);
	}

	/**
	 * Return the value of the {@code Allow} header for the given URI.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param urlVariables the variables to expand in the template
	 * @return the value of the {@code Allow} header
	 * @see RestTemplate#optionsForAllow(java.lang.String, java.util.Map)
	 */
	public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> urlVariables) {
		return this.restTemplate.optionsForAllow(url, urlVariables);
	}

	/**
	 * Return the value of the {@code Allow} header for the given URL.
	 * @param url the URL
	 * @return the value of the {@code Allow} header
	 * @see RestTemplate#optionsForAllow(java.net.URI)
	 */
	public Set<HttpMethod> optionsForAllow(URI url) {
		return this.restTemplate.optionsForAllow(applyRootUriIfNecessary(url));
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity, java.lang.Class, java.lang.Object[])
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Object... urlVariables) {
		return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity, java.lang.Class, java.util.Map)
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType, Map<String, ?> urlVariables) {
		return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(java.net.URI, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity, java.lang.Class)
	 */
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			Class<T> responseType) {
		return this.restTemplate.exchange(applyRootUriIfNecessary(url), method, requestEntity, responseType);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}. The given
	 * {@link ParameterizedTypeReference} is used to pass generic type information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity,
	 * org.springframework.core.ParameterizedTypeReference, java.lang.Object[])
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Object... urlVariables) {
		return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}. The given
	 * {@link ParameterizedTypeReference} is used to pass generic type information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity,
	 * org.springframework.core.ParameterizedTypeReference, java.util.Map)
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType, Map<String, ?> urlVariables) {
		return this.restTemplate.exchange(url, method, requestEntity, responseType, urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}. The given
	 * {@link ParameterizedTypeReference} is used to pass generic type information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;https://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(java.net.URI, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity,
	 * org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method, @Nullable HttpEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType) {
		return this.restTemplate.exchange(applyRootUriIfNecessary(url), method, requestEntity, responseType);
	}

	/**
	 * Execute the request specified in the given {@link RequestEntity} and return the
	 * response as {@link ResponseEntity}. Typically used in combination with the static
	 * builder methods on {@code RequestEntity}, for instance: <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity.post(new URI(&quot;https://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
	 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
	 * </pre>
	 * @param requestEntity the entity to write to the request
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(org.springframework.http.RequestEntity, java.lang.Class)
	 */
	public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, Class<T> responseType) {
		return this.restTemplate.exchange(createRequestEntityWithRootAppliedUri(requestEntity), responseType);
	}

	/**
	 * Execute the request specified in the given {@link RequestEntity} and return the
	 * response as {@link ResponseEntity}. The given {@link ParameterizedTypeReference} is
	 * used to pass generic type information: <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity.post(new URI(&quot;https://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
	 * ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
	 * </pre>
	 * @param requestEntity the entity to write to the request
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @see RestTemplate#exchange(org.springframework.http.RequestEntity,
	 * org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity, ParameterizedTypeReference<T> responseType) {
		return this.restTemplate.exchange(createRequestEntityWithRootAppliedUri(requestEntity), responseType);
	}

	/**
	 * Execute the HTTP method to the given URI template, preparing the request with the
	 * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 * @see RestTemplate#execute(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.web.client.RequestCallback,
	 * org.springframework.web.client.ResponseExtractor, java.lang.Object[])
	 */
	public <T> @Nullable T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Object... urlVariables) {
		return this.restTemplate.execute(url, method, requestCallback, responseExtractor, urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, preparing the request with the
	 * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables map.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 * @see RestTemplate#execute(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.web.client.RequestCallback,
	 * org.springframework.web.client.ResponseExtractor, java.util.Map)
	 */
	public <T> @Nullable T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Map<String, ?> urlVariables) {
		return this.restTemplate.execute(url, method, requestCallback, responseExtractor, urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URL, preparing the request with the
	 * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc.)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param <T> the type of the return value
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 * @see RestTemplate#execute(java.net.URI, org.springframework.http.HttpMethod,
	 * org.springframework.web.client.RequestCallback,
	 * org.springframework.web.client.ResponseExtractor)
	 */
	public <T> @Nullable T execute(URI url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) {
		return this.restTemplate.execute(applyRootUriIfNecessary(url), method, requestCallback, responseExtractor);
	}

	/**
	 * Returns the underlying {@link RestTemplate} that is actually used to perform the
	 * REST operations.
	 * @return the restTemplate
	 */
	public RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	/**
	 * Creates a new {@code TestRestTemplate} with the same configuration as this one,
	 * except that it will send basic authorization headers using the given
	 * {@code username} and {@code password}. The request factory used is a new instance
	 * of the underlying {@link RestTemplate}'s request factory type (when possible).
	 * @param username the username
	 * @param password the password
	 * @return the new template
	 */
	public TestRestTemplate withBasicAuth(@Nullable String username, @Nullable String password) {
		if (username == null || password == null) {
			return this;
		}
		return new TestRestTemplate(this.builder.basicAuthentication(username, password),
				this.restTemplate.getUriTemplateHandler());
	}

	/**
	 * Creates a new {@code TestRestTemplate} with the same configuration as this one,
	 * except that it will apply the given {@link HttpRedirects}. The request factory used
	 * is a new instance of the underlying {@link RestTemplate}'s request factory type
	 * (when possible).
	 * @param redirects the new redirect settings
	 * @return the new template
	 */
	public TestRestTemplate withRedirects(HttpRedirects redirects) {
		return withClientSettings((settings) -> settings.withRedirects(redirects));
	}

	/**
	 * Creates a new {@code TestRestTemplate} with the same configuration as this one,
	 * except that it will apply the given {@link HttpClientSettings}. The request factory
	 * used is a new instance of the underlying {@link RestTemplate}'s request factory
	 * type (when possible).
	 * @param clientSettings the new client settings
	 * @return the new template
	 * @since 4.0.0
	 */
	public TestRestTemplate withClientSettings(HttpClientSettings clientSettings) {
		return new TestRestTemplate(this.builder.clientSettings(clientSettings),
				this.restTemplate.getUriTemplateHandler());
	}

	/**
	 * Creates a new {@code TestRestTemplate} with the same configuration as this one,
	 * except that it will customize the {@link HttpClientSettings}. The request factory
	 * used is a new instance of the underlying {@link RestTemplate}'s request factory
	 * type (when possible).
	 * @param clientSettingsCustomizer a {@link UnaryOperator} to update the settings
	 * @return the new template
	 * @since 4.0.0
	 */
	public TestRestTemplate withClientSettings(UnaryOperator<HttpClientSettings> clientSettingsCustomizer) {
		return new TestRestTemplate(this.builder.clientSettings(clientSettingsCustomizer),
				this.restTemplate.getUriTemplateHandler());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private RequestEntity<?> createRequestEntityWithRootAppliedUri(RequestEntity<?> requestEntity) {
		return new RequestEntity(requestEntity.getBody(), requestEntity.getHeaders(), requestEntity.getMethod(),
				applyRootUriIfNecessary(resolveUri(requestEntity)), requestEntity.getType());
	}

	private URI applyRootUriIfNecessary(URI uri) {
		if (!uri.toString().startsWith("/")) {
			return uri;
		}
		return URI.create(this.restTemplate.getUriTemplateHandler().expand("/") + uri.toString().substring(1));
	}

	private URI resolveUri(RequestEntity<?> entity) {
		if (entity instanceof UriTemplateRequestEntity<?> templatedUriEntity) {
			if (templatedUriEntity.getVars() != null) {
				return this.restTemplate.getUriTemplateHandler()
					.expand(templatedUriEntity.getUriTemplate(), templatedUriEntity.getVars());
			}
			else if (templatedUriEntity.getVarsMap() != null) {
				return this.restTemplate.getUriTemplateHandler()
					.expand(templatedUriEntity.getUriTemplate(), templatedUriEntity.getVarsMap());
			}
			throw new IllegalStateException(
					"No variables specified for URI template: " + templatedUriEntity.getUriTemplate());
		}
		return entity.getUrl();
	}

	/**
	 * Options used to customize the Apache HTTP Client.
	 */
	public enum HttpClientOption {

		/**
		 * Enable cookies.
		 */
		ENABLE_COOKIES,

		/**
		 * Use a {@link TlsSocketStrategy} that trusts self-signed certificates.
		 */
		SSL;

		boolean isPresent(HttpClientOption[] options) {
			return ObjectUtils.containsElement(options, this);
		}

	}

	/**
	 * Factory used to create a {@link TlsSocketStrategy} supporting self-signed
	 * certificates.
	 */
	private static final class SelfSignedTlsSocketStrategyFactory implements TlsSocketStrategyFactory {

		private static final String[] SUPPORTED_PROTOCOLS = { TLS.V_1_3.getId(), TLS.V_1_2.getId() };

		@Override
		public TlsSocketStrategy getTlsSocketStrategy(@Nullable SslBundle sslBundle) {
			try {
				TrustSelfSignedStrategy trustStrategy = new TrustSelfSignedStrategy();
				SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
				return new DefaultClientTlsStrategy(sslContext, SUPPORTED_PROTOCOLS, null, null, null);
			}
			catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	/**
	 * {@link TrustStrategy} supporting self-signed certificates.
	 */
	private static final class TrustSelfSignedStrategy implements TrustStrategy {

		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType) {
			return chain.length == 1;
		}

	}

	private static class CookieSpecCustomizer implements Consumer<RequestConfig.Builder> {

		private final boolean enableCookies;

		CookieSpecCustomizer(boolean enableCookies) {
			this.enableCookies = enableCookies;
		}

		@Override
		public void accept(RequestConfig.Builder builder) {
			builder.setCookieSpec(this.enableCookies ? StandardCookieSpec.STRICT : StandardCookieSpec.IGNORE);
		}

	}

}
