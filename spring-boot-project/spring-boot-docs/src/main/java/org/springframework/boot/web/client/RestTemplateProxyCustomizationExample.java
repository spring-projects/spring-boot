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

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Example configuration for using a {@link RestTemplateCustomizer} to configure a proxy.
 *
 * @author Andy Wilkinson
 */
public class RestTemplateProxyCustomizationExample {

	/**
	 * A {@link RestTemplateCustomizer} that applies an HttpComponents-based request
	 * factory that is configured to use a proxy.
	 */
	// tag::customizer[]
	static class ProxyCustomizer implements RestTemplateCustomizer {

		@Override
		public void customize(RestTemplate restTemplate) {
			HttpHost proxy = new HttpHost("proxy.example.com");
			HttpClient httpClient = HttpClientBuilder.create()
					.setRoutePlanner(new DefaultProxyRoutePlanner(proxy) {

						@Override
						public HttpHost determineProxy(HttpHost target,
								HttpRequest request, HttpContext context)
										throws HttpException {
							if (target.getHostName().equals("192.168.0.5")) {
								return null;
							}
							return super.determineProxy(target, request, context);
						}

					}).build();
			restTemplate.setRequestFactory(
					new HttpComponentsClientHttpRequestFactory(httpClient));
		}

	}
	// end::customizer[]

}
