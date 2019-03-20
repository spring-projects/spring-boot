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

package org.springframework.boot.test.web.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.client.ClientHttpRequestFactorySupplier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

/**
 * Convenient alternative of {@link RestTemplate} that is suitable for integration tests.
 * They are fault tolerant, and optionally can carry Basic authentication headers. If
 * Apache Http Client 4.3.2 or better is available (recommended) it will be used as the
 * client, and by default configured to ignore cookies and redirects.
 * <p>
 * Note: To prevent injection problems this class intentionally does not extend
 * {@link RestTemplate}. If you need access to the underlying {@link RestTemplate} use
 * {@link #getRestTemplate()}.
 * <p>
 * If you are using the
 * {@link org.springframework.boot.test.context.SpringBootTest @SpringBootTest}
 * annotation, a {@link TestRestTemplate} is automatically available and can be
 * {@code @Autowired} into your test. If you need customizations (for example to adding
 * additional message converters) use a {@link RestTemplateBuilder} {@code @Bean}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kristine Jetzke
 * @since 1.4.0
 */
public class TestRestTemplate {

	private final RestTemplate restTemplate;

	private final HttpClientOption[] httpClientOptions;

	/**
	 * Create a new {@link TestRestTemplate} instance.
	 * @param restTemplateBuilder builder used to configure underlying
	 * {@link RestTemplate}
	 * @since 1.4.1
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
	public TestRestTemplate(String username, String password,
			HttpClientOption... httpClientOptions) {
		this(new RestTemplateBuilder(), username, password, httpClientOptions);
	}

	/**
	 * Create a new {@link TestRestTemplate} instance with the specified credentials.
	 * @param restTemplateBuilder builder used to configure underlying
	 * {@link RestTemplate}
	 * @param username the username to use (or {@code null})
	 * @param password the password (or {@code null})
	 * @param httpClientOptions client options to use if the Apache HTTP Client is used
	 * @since 2.0.0
	 */
	public TestRestTemplate(RestTemplateBuilder restTemplateBuilder, String username,
			String password, HttpClientOption... httpClientOptions) {
		this((restTemplateBuilder != null) ? restTemplateBuilder.build() : null, username,
				password, httpClientOptions);
	}

	private TestRestTemplate(RestTemplate restTemplate, String username, String password,
			HttpClientOption... httpClientOptions) {
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		this.httpClientOptions = httpClientOptions;
		if (getRequestFactoryClass(restTemplate)
				.isAssignableFrom(HttpComponentsClientHttpRequestFactory.class)) {
			restTemplate.setRequestFactory(
					new CustomHttpComponentsClientHttpRequestFactory(httpClientOptions));
		}
		addAuthentication(restTemplate, username, password);
		restTemplate.setErrorHandler(new NoOpResponseErrorHandler());
		this.restTemplate = restTemplate;
	}

	private Class<? extends ClientHttpRequestFactory> getRequestFactoryClass(
			RestTemplate restTemplate) {
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		if (InterceptingClientHttpRequestFactory.class
				.isAssignableFrom(requestFactory.getClass())) {
			Field requestFactoryField = ReflectionUtils.findField(RestTemplate.class,
					"requestFactory");
			ReflectionUtils.makeAccessible(requestFactoryField);
			requestFactory = (ClientHttpRequestFactory) ReflectionUtils
					.getField(requestFactoryField, restTemplate);
		}
		return requestFactory.getClass();
	}

	private void addAuthentication(RestTemplate restTemplate, String username,
			String password) {
		if (username == null) {
			return;
		}
		List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
		if (interceptors == null) {
			interceptors = Collections.emptyList();
		}
		interceptors = new ArrayList<>(interceptors);
		interceptors.removeIf(BasicAuthenticationInterceptor.class::isInstance);
		interceptors.add(new BasicAuthenticationInterceptor(username, password));
		restTemplate.setInterceptors(interceptors);
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
	 * Returns the root URI applied by a {@link RootUriTemplateHandler} or {@code ""} if
	 * the root URI is not available.
	 * @return the root URI
	 */
	public String getRootUri() {
		UriTemplateHandler uriTemplateHandler = this.restTemplate.getUriTemplateHandler();
		if (uriTemplateHandler instanceof RootUriTemplateHandler) {
			return ((RootUriTemplateHandler) uriTemplateHandler).getRootUri();
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
	 * @throws RestClientException on client-side HTTP error on client-side HTTP error
	 * @see RestTemplate#getForObject(String, Class, Object...)
	 */
	public <T> T getForObject(String url, Class<T> responseType, Object... urlVariables)
			throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#getForObject(String, Class, Object...)
	 */
	public <T> T getForObject(String url, Class<T> responseType,
			Map<String, ?> urlVariables) throws RestClientException {
		return this.restTemplate.getForObject(url, responseType, urlVariables);
	}

	/**
	 * Retrieve a representation by doing a GET on the URL . The response (if any) is
	 * converted and returned.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#getForObject(java.net.URI, java.lang.Class)
	 */
	public <T> T getForObject(URI url, Class<T> responseType) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#getForEntity(java.lang.String, java.lang.Class,
	 * java.lang.Object[])
	 */
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType,
			Object... urlVariables) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#getForEntity(java.lang.String, java.lang.Class, java.util.Map)
	 */
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType,
			Map<String, ?> urlVariables) throws RestClientException {
		return this.restTemplate.getForEntity(url, responseType, urlVariables);
	}

	/**
	 * Retrieve a representation by doing a GET on the URL . The response is converted and
	 * stored in an {@link ResponseEntity}.
	 * @param url the URL
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the converted object
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#getForEntity(java.net.URI, java.lang.Class)
	 */
	public <T> ResponseEntity<T> getForEntity(URI url, Class<T> responseType)
			throws RestClientException {
		return this.restTemplate.getForEntity(applyRootUriIfNecessary(url), responseType);
	}

	/**
	 * Retrieve all headers of the resource specified by the URI template.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param urlVariables the variables to expand the template
	 * @return all HTTP headers of that resource
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#headForHeaders(java.lang.String, java.lang.Object[])
	 */
	public HttpHeaders headForHeaders(String url, Object... urlVariables)
			throws RestClientException {
		return this.restTemplate.headForHeaders(url, urlVariables);
	}

	/**
	 * Retrieve all headers of the resource specified by the URI template.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param urlVariables the map containing variables for the URI template
	 * @return all HTTP headers of that resource
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#headForHeaders(java.lang.String, java.util.Map)
	 */
	public HttpHeaders headForHeaders(String url, Map<String, ?> urlVariables)
			throws RestClientException {
		return this.restTemplate.headForHeaders(url, urlVariables);
	}

	/**
	 * Retrieve all headers of the resource specified by the URL.
	 * @param url the URL
	 * @return all HTTP headers of that resource
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#headForHeaders(java.net.URI)
	 */
	public HttpHeaders headForHeaders(URI url) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForLocation(java.lang.String, java.lang.Object,
	 * java.lang.Object[])
	 */
	public URI postForLocation(String url, Object request, Object... urlVariables)
			throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForLocation(java.lang.String, java.lang.Object,
	 * java.util.Map)
	 */
	public URI postForLocation(String url, Object request, Map<String, ?> urlVariables)
			throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForLocation(java.net.URI, java.lang.Object)
	 */
	public URI postForLocation(URI url, Object request) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForObject(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.lang.Object[])
	 */
	public <T> T postForObject(String url, Object request, Class<T> responseType,
			Object... urlVariables) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForObject(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.util.Map)
	 */
	public <T> T postForObject(String url, Object request, Class<T> responseType,
			Map<String, ?> urlVariables) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForObject(java.net.URI, java.lang.Object, java.lang.Class)
	 */
	public <T> T postForObject(URI url, Object request, Class<T> responseType)
			throws RestClientException {
		return this.restTemplate.postForObject(applyRootUriIfNecessary(url), request,
				responseType);
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForEntity(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.lang.Object[])
	 */
	public <T> ResponseEntity<T> postForEntity(String url, Object request,
			Class<T> responseType, Object... urlVariables) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForEntity(java.lang.String, java.lang.Object,
	 * java.lang.Class, java.util.Map)
	 */
	public <T> ResponseEntity<T> postForEntity(String url, Object request,
			Class<T> responseType, Map<String, ?> urlVariables)
			throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#postForEntity(java.net.URI, java.lang.Object, java.lang.Class)
	 */
	public <T> ResponseEntity<T> postForEntity(URI url, Object request,
			Class<T> responseType) throws RestClientException {
		return this.restTemplate.postForEntity(applyRootUriIfNecessary(url), request,
				responseType);
	}

	/**
	 * Create or update a resource by PUTting the given object to the URI.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @param urlVariables the variables to expand the template
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#put(java.lang.String, java.lang.Object, java.lang.Object[])
	 */
	public void put(String url, Object request, Object... urlVariables)
			throws RestClientException {
		this.restTemplate.put(url, request, urlVariables);
	}

	/**
	 * Creates a new resource by PUTting the given object to URI template.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @param urlVariables the variables to expand the template
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#put(java.lang.String, java.lang.Object, java.util.Map)
	 */
	public void put(String url, Object request, Map<String, ?> urlVariables)
			throws RestClientException {
		this.restTemplate.put(url, request, urlVariables);
	}

	/**
	 * Creates a new resource by PUTting the given object to URL.
	 * <p>
	 * The {@code request} parameter can be a {@link HttpEntity} in order to add
	 * additional HTTP headers to the request.
	 * @param url the URL
	 * @param request the Object to be PUT, may be {@code null}
	 * @throws RestClientException on client-side HTTP error
	 * @see HttpEntity
	 * @see RestTemplate#put(java.net.URI, java.lang.Object)
	 */
	public void put(URI url, Object request) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @since 1.4.4
	 * @see HttpEntity
	 */
	public <T> T patchForObject(String url, Object request, Class<T> responseType,
			Object... uriVariables) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @since 1.4.4
	 * @see HttpEntity
	 */
	public <T> T patchForObject(String url, Object request, Class<T> responseType,
			Map<String, ?> uriVariables) throws RestClientException {
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
	 * @throws RestClientException on client-side HTTP error
	 * @since 1.4.4
	 * @see HttpEntity
	 */
	public <T> T patchForObject(URI url, Object request, Class<T> responseType)
			throws RestClientException {
		return this.restTemplate.patchForObject(applyRootUriIfNecessary(url), request,
				responseType);

	}

	/**
	 * Delete the resources at the specified URI.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param urlVariables the variables to expand in the template
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#delete(java.lang.String, java.lang.Object[])
	 */
	public void delete(String url, Object... urlVariables) throws RestClientException {
		this.restTemplate.delete(url, urlVariables);
	}

	/**
	 * Delete the resources at the specified URI.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param urlVariables the variables to expand the template
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#delete(java.lang.String, java.util.Map)
	 */
	public void delete(String url, Map<String, ?> urlVariables)
			throws RestClientException {
		this.restTemplate.delete(url, urlVariables);
	}

	/**
	 * Delete the resources at the specified URL.
	 * @param url the URL
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#delete(java.net.URI)
	 */
	public void delete(URI url) throws RestClientException {
		this.restTemplate.delete(applyRootUriIfNecessary(url));
	}

	/**
	 * Return the value of the Allow header for the given URI.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param urlVariables the variables to expand in the template
	 * @return the value of the allow header
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#optionsForAllow(java.lang.String, java.lang.Object[])
	 */
	public Set<HttpMethod> optionsForAllow(String url, Object... urlVariables)
			throws RestClientException {
		return this.restTemplate.optionsForAllow(url, urlVariables);
	}

	/**
	 * Return the value of the Allow header for the given URI.
	 * <p>
	 * URI Template variables are expanded using the given map.
	 * @param url the URL
	 * @param urlVariables the variables to expand in the template
	 * @return the value of the allow header
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#optionsForAllow(java.lang.String, java.util.Map)
	 */
	public Set<HttpMethod> optionsForAllow(String url, Map<String, ?> urlVariables)
			throws RestClientException {
		return this.restTemplate.optionsForAllow(url, urlVariables);
	}

	/**
	 * Return the value of the Allow header for the given URL.
	 * @param url the URL
	 * @return the value of the allow header
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#optionsForAllow(java.net.URI)
	 */
	public Set<HttpMethod> optionsForAllow(URI url) throws RestClientException {
		return this.restTemplate.optionsForAllow(applyRootUriIfNecessary(url));
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity, java.lang.Class, java.lang.Object[])
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType, Object... urlVariables)
			throws RestClientException {
		return this.restTemplate.exchange(url, method, requestEntity, responseType,
				urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity, java.lang.Class, java.util.Map)
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType,
			Map<String, ?> urlVariables) throws RestClientException {
		return this.restTemplate.exchange(url, method, requestEntity, responseType,
				urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(java.net.URI, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity, java.lang.Class)
	 */
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, Class<T> responseType)
			throws RestClientException {
		return this.restTemplate.exchange(applyRootUriIfNecessary(url), method,
				requestEntity, responseType);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}. The given
	 * {@link ParameterizedTypeReference} is used to pass generic type information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity,
	 * org.springframework.core.ParameterizedTypeReference, java.lang.Object[])
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Object... urlVariables) throws RestClientException {
		return this.restTemplate.exchange(url, method, requestEntity, responseType,
				urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}. The given
	 * {@link ParameterizedTypeReference} is used to pass generic type information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity,
	 * org.springframework.core.ParameterizedTypeReference, java.util.Map)
	 */
	public <T> ResponseEntity<T> exchange(String url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType,
			Map<String, ?> urlVariables) throws RestClientException {
		return this.restTemplate.exchange(url, method, requestEntity, responseType,
				urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, writing the given request entity
	 * to the request, and returns the response as {@link ResponseEntity}. The given
	 * {@link ParameterizedTypeReference} is used to pass generic type information:
	 * <pre class="code">
	 * ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyBean&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyBean&gt;&gt; response = template.exchange(&quot;http://example.com&quot;,HttpMethod.GET, null, myBean);
	 * </pre>
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestEntity the entity (headers and/or body) to write to the request, may
	 * be {@code null}
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(java.net.URI, org.springframework.http.HttpMethod,
	 * org.springframework.http.HttpEntity,
	 * org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> ResponseEntity<T> exchange(URI url, HttpMethod method,
			HttpEntity<?> requestEntity, ParameterizedTypeReference<T> responseType)
			throws RestClientException {
		return this.restTemplate.exchange(applyRootUriIfNecessary(url), method,
				requestEntity, responseType);
	}

	/**
	 * Execute the request specified in the given {@link RequestEntity} and return the
	 * response as {@link ResponseEntity}. Typically used in combination with the static
	 * builder methods on {@code RequestEntity}, for instance: <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity.post(new URI(&quot;http://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
	 * ResponseEntity&lt;MyResponse&gt; response = template.exchange(request, MyResponse.class);
	 * </pre>
	 * @param requestEntity the entity to write to the request
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(org.springframework.http.RequestEntity, java.lang.Class)
	 */
	public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity,
			Class<T> responseType) throws RestClientException {
		return this.restTemplate.exchange(
				createRequestEntityWithRootAppliedUri(requestEntity), responseType);
	}

	/**
	 * Execute the request specified in the given {@link RequestEntity} and return the
	 * response as {@link ResponseEntity}. The given {@link ParameterizedTypeReference} is
	 * used to pass generic type information: <pre class="code">
	 * MyRequest body = ...
	 * RequestEntity request = RequestEntity.post(new URI(&quot;http://example.com/foo&quot;)).accept(MediaType.APPLICATION_JSON).body(body);
	 * ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt; myBean = new ParameterizedTypeReference&lt;List&lt;MyResponse&gt;&gt;() {};
	 * ResponseEntity&lt;List&lt;MyResponse&gt;&gt; response = template.exchange(request, myBean);
	 * </pre>
	 * @param requestEntity the entity to write to the request
	 * @param responseType the type of the return value
	 * @param <T> the type of the return value
	 * @return the response as entity
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#exchange(org.springframework.http.RequestEntity,
	 * org.springframework.core.ParameterizedTypeReference)
	 */
	public <T> ResponseEntity<T> exchange(RequestEntity<?> requestEntity,
			ParameterizedTypeReference<T> responseType) throws RestClientException {
		return this.restTemplate.exchange(
				createRequestEntityWithRootAppliedUri(requestEntity), responseType);
	}

	/**
	 * Execute the HTTP method to the given URI template, preparing the request with the
	 * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables, if any.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#execute(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.web.client.RequestCallback,
	 * org.springframework.web.client.ResponseExtractor, java.lang.Object[])
	 */
	public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Object... urlVariables)
			throws RestClientException {
		return this.restTemplate.execute(url, method, requestCallback, responseExtractor,
				urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URI template, preparing the request with the
	 * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
	 * <p>
	 * URI Template variables are expanded using the given URI variables map.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param urlVariables the variables to expand in the template
	 * @param <T> the type of the return value
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#execute(java.lang.String, org.springframework.http.HttpMethod,
	 * org.springframework.web.client.RequestCallback,
	 * org.springframework.web.client.ResponseExtractor, java.util.Map)
	 */
	public <T> T execute(String url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, Map<String, ?> urlVariables)
			throws RestClientException {
		return this.restTemplate.execute(url, method, requestCallback, responseExtractor,
				urlVariables);
	}

	/**
	 * Execute the HTTP method to the given URL, preparing the request with the
	 * {@link RequestCallback}, and reading the response with a {@link ResponseExtractor}.
	 * @param url the URL
	 * @param method the HTTP method (GET, POST, etc)
	 * @param requestCallback object that prepares the request
	 * @param responseExtractor object that extracts the return value from the response
	 * @param <T> the type of the return value
	 * @return an arbitrary object, as returned by the {@link ResponseExtractor}
	 * @throws RestClientException on client-side HTTP error
	 * @see RestTemplate#execute(java.net.URI, org.springframework.http.HttpMethod,
	 * org.springframework.web.client.RequestCallback,
	 * org.springframework.web.client.ResponseExtractor)
	 */
	public <T> T execute(URI url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {
		return this.restTemplate.execute(applyRootUriIfNecessary(url), method,
				requestCallback, responseExtractor);
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
	 * @since 1.4.1
	 */
	public TestRestTemplate withBasicAuth(String username, String password) {
		RestTemplate restTemplate = new RestTemplateBuilder()
				.requestFactory(getRequestFactorySupplier())
				.messageConverters(getRestTemplate().getMessageConverters())
				.interceptors(getRestTemplate().getInterceptors())
				.uriTemplateHandler(getRestTemplate().getUriTemplateHandler()).build();
		return new TestRestTemplate(restTemplate, username, password,
				this.httpClientOptions);
	}

	private Supplier<ClientHttpRequestFactory> getRequestFactorySupplier() {
		return () -> {
			try {
				return BeanUtils
						.instantiateClass(getRequestFactoryClass(getRestTemplate()));
			}
			catch (BeanInstantiationException ex) {
				return new ClientHttpRequestFactorySupplier().get();
			}
		};
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private RequestEntity<?> createRequestEntityWithRootAppliedUri(
			RequestEntity<?> requestEntity) {
		return new RequestEntity(requestEntity.getBody(), requestEntity.getHeaders(),
				requestEntity.getMethod(),
				applyRootUriIfNecessary(requestEntity.getUrl()), requestEntity.getType());
	}

	private URI applyRootUriIfNecessary(URI uri) {
		UriTemplateHandler uriTemplateHandler = this.restTemplate.getUriTemplateHandler();
		if ((uriTemplateHandler instanceof RootUriTemplateHandler)
				&& uri.toString().startsWith("/")) {
			return URI.create(((RootUriTemplateHandler) uriTemplateHandler).getRootUri()
					+ uri.toString());
		}
		return uri;
	}

	/**
	 * Options used to customize the Apache Http Client if it is used.
	 */
	public enum HttpClientOption {

		/**
		 * Enable cookies.
		 */
		ENABLE_COOKIES,

		/**
		 * Enable redirects.
		 */
		ENABLE_REDIRECTS,

		/**
		 * Use a {@link SSLConnectionSocketFactory} with {@link TrustSelfSignedStrategy}.
		 */
		SSL

	}

	/**
	 * {@link HttpComponentsClientHttpRequestFactory} to apply customizations.
	 */
	protected static class CustomHttpComponentsClientHttpRequestFactory
			extends HttpComponentsClientHttpRequestFactory {

		private final String cookieSpec;

		private final boolean enableRedirects;

		public CustomHttpComponentsClientHttpRequestFactory(
				HttpClientOption[] httpClientOptions) {
			Set<HttpClientOption> options = new HashSet<>(
					Arrays.asList(httpClientOptions));
			this.cookieSpec = (options.contains(HttpClientOption.ENABLE_COOKIES)
					? CookieSpecs.STANDARD : CookieSpecs.IGNORE_COOKIES);
			this.enableRedirects = options.contains(HttpClientOption.ENABLE_REDIRECTS);
			if (options.contains(HttpClientOption.SSL)) {
				setHttpClient(createSslHttpClient());
			}
		}

		private HttpClient createSslHttpClient() {
			try {
				SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
						new SSLContextBuilder()
								.loadTrustMaterial(null, new TrustSelfSignedStrategy())
								.build());
				return HttpClients.custom().setSSLSocketFactory(socketFactory).build();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to create SSL HttpClient", ex);
			}
		}

		@Override
		protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
			HttpClientContext context = HttpClientContext.create();
			context.setRequestConfig(getRequestConfig());
			return context;
		}

		protected RequestConfig getRequestConfig() {
			Builder builder = RequestConfig.custom().setCookieSpec(this.cookieSpec)
					.setAuthenticationEnabled(false)
					.setRedirectsEnabled(this.enableRedirects);
			return builder.build();
		}

	}

	private static class NoOpResponseErrorHandler extends DefaultResponseErrorHandler {

		@Override
		public void handleError(ClientHttpResponse response) throws IOException {
		}

	}

}
