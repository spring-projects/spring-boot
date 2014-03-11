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
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Convenient static factory for {@link RestTemplate} instances that are suitable for
 * integration tests. They are fault tolerant, ignore cookies, and optionally can carry
 * Basic authentication headers.
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
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;

	}

}
