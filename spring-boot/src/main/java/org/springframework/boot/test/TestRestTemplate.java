/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Convenient subclass of {@link RestTemplate} that is suitable for integration tests.
 * They are fault tolerant, and optionally can carry Basic authentication headers. If
 * Apache Http Client 4.3.2 or better is available (recommended) it will be used as the
 * client, and by default configured to ignore cookies and redirects.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class TestRestTemplate extends RestTemplate {

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
		if (ClassUtils.isPresent("org.apache.http.client.config.RequestConfig", null)) {
			setRequestFactory(new CustomHttpComponentsClientHttpRequestFactory(
					httpClientOptions));
		}
		addAuthentication(username, password);
		setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});

	}

	private void addAuthentication(String username, String password) {
		if (username == null) {
			return;
		}
		List<ClientHttpRequestInterceptor> interceptors = Collections
				.<ClientHttpRequestInterceptor> singletonList(new BasicAuthorizationInterceptor(
						username, password));
		setRequestFactory(new InterceptingClientHttpRequestFactory(getRequestFactory(),
				interceptors));
	}

	/**
	 * Options used to customize the Apache Http Client if it is used.
	 */
	public static enum HttpClientOption {

		/**
		 * Enable cookies.
		 */
		ENABLE_COOKIES,

		/**
		 * Enable redirects.
		 */
		ENABLE_REDIRECTS

	}

	private static class BasicAuthorizationInterceptor implements
			ClientHttpRequestInterceptor {

		private final String username;

		private final String password;

		public BasicAuthorizationInterceptor(String username, String password) {
			this.username = username;
			this.password = (password == null ? "" : password);
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			String token = Base64Encoder.encode(this.username + ":" + this.password);
			request.getHeaders().add("Authorization", "Basic " + token);
			return execution.execute(request, body);
		}

	}

	protected static class CustomHttpComponentsClientHttpRequestFactory extends
			HttpComponentsClientHttpRequestFactory {

		private final String cookieSpec;

		private final boolean enableRedirects;

		public CustomHttpComponentsClientHttpRequestFactory(
				HttpClientOption[] httpClientOptions) {
			Set<HttpClientOption> options = new HashSet<TestRestTemplate.HttpClientOption>(
					Arrays.asList(httpClientOptions));
			this.cookieSpec = (options.contains(HttpClientOption.ENABLE_COOKIES) ? CookieSpecs.STANDARD
					: CookieSpecs.IGNORE_COOKIES);
			this.enableRedirects = options.contains(HttpClientOption.ENABLE_REDIRECTS);
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

}
