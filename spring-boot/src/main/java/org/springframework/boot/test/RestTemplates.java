/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Convenient static factory for {@link RestTemplate} instances that are suitable for
 * integration tests. They are fault tolerant, and optionally can carry Basic
 * authentication headers. If Apache Http Client 4.3.2 or better is available
 * (recommended) it will be used as the client, and configured to ignore cookies and
 * redirects.
 * 
 * @author Dave Syer
 */
public class RestTemplates {

	/**
	 * Basic factory method for a RestTemplate that does not follow redirects, ignores
	 * cookies and does not throw exceptions on server side errors.
	 * 
	 * @return a basic RestTemplate with no authentication
	 */
	public static RestTemplate get() {
		return get(null, null);
	}

	/**
	 * Factory method for a secure RestTemplate with Basic authentication that does not
	 * follow redirects, ignores cookies and does not throw exceptions on server side
	 * errors.
	 * 
	 * @return a basic RestTemplate with Basic authentication
	 */
	public static RestTemplate get(final String username, final String password) {

		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();

		if (username != null) {

			interceptors.add(new ClientHttpRequestInterceptor() {

				@Override
				public ClientHttpResponse intercept(HttpRequest request, byte[] body,
						ClientHttpRequestExecution execution) throws IOException {
					request.getHeaders().add(
							"Authorization",
							"Basic "
									+ new String(Base64
											.encode((username + ":" + password)
													.getBytes())));
					return execution.execute(request, body);
				}
			});
		}

		RestTemplate restTemplate = new RestTemplate(
				new InterceptingClientHttpRequestFactory(
						new SimpleClientHttpRequestFactory(), interceptors));
		if (ClassUtils.isPresent("org.apache.http.client.config.RequestConfig", null)) {
			new HttpComponentsCustomizer().customize(restTemplate);
		}
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;

	}

	private static class HttpComponentsCustomizer {

		public void customize(RestTemplate restTemplate) {
			restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
				@Override
				protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
					HttpClientContext context = HttpClientContext.create();
					Builder builder = RequestConfig.custom()
							.setCookieSpec(CookieSpecs.IGNORE_COOKIES)
							.setAuthenticationEnabled(false).setRedirectsEnabled(false);
					context.setRequestConfig(builder.build());
					return context;
				}
			});
		}

	}

}
