/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.resttemplate.customization;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class MyRestTemplateCustomizer implements RestTemplateCustomizer {

	@Override
	public void customize(RestTemplate restTemplate) {
		HttpRoutePlanner routePlanner = new CustomRoutePlanner(new HttpHost("proxy.example.com"));
		HttpClient httpClient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build();
		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
	}

	static class CustomRoutePlanner extends DefaultProxyRoutePlanner {

		CustomRoutePlanner(HttpHost proxy) {
			super(proxy);
		}

		@Override
		protected HttpHost determineProxy(HttpHost target, HttpContext context) throws HttpException {
			if (target.getHostName().equals("192.168.0.5")) {
				return null;
			}
			return super.determineProxy(target, context);
		}

	}

}
