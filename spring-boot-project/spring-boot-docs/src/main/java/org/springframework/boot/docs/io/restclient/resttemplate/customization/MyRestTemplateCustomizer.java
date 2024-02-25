/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.io.restclient.resttemplate.customization;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * MyRestTemplateCustomizer class.
 */
public class MyRestTemplateCustomizer implements RestTemplateCustomizer {

	/**
	 * Customizes the RestTemplate by setting a custom route planner and request factory.
	 * @param restTemplate the RestTemplate to be customized
	 */
	@Override
	public void customize(RestTemplate restTemplate) {
		HttpRoutePlanner routePlanner = new CustomRoutePlanner(new HttpHost("proxy.example.com"));
		HttpClient httpClient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build();
		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
	}

	/**
	 * CustomRoutePlanner class.
	 */
	static class CustomRoutePlanner extends DefaultProxyRoutePlanner {

		/**
		 * Constructs a new CustomRoutePlanner with the specified proxy.
		 * @param proxy the HTTP proxy to be used by the route planner
		 */
		CustomRoutePlanner(HttpHost proxy) {
			super(proxy);
		}

		/**
		 * Determines the proxy to be used for the given target host.
		 * @param target the target host
		 * @param context the HTTP context
		 * @return the proxy to be used, or null if no proxy should be used
		 * @throws HttpException if an HTTP exception occurs
		 */
		@Override
		protected HttpHost determineProxy(HttpHost target, HttpContext context) throws HttpException {
			if (target.getHostName().equals("192.168.0.5")) {
				return null;
			}
			return super.determineProxy(target, context);
		}

	}

}
