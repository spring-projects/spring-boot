/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.resttemplate.customization

import org.apache.hc.client5.http.classic.HttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner
import org.apache.hc.client5.http.routing.HttpRoutePlanner
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.protocol.HttpContext
import org.springframework.boot.restclient.RestTemplateCustomizer
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

class MyRestTemplateCustomizer : RestTemplateCustomizer {

	override fun customize(restTemplate: RestTemplate) {
		val routePlanner: HttpRoutePlanner = CustomRoutePlanner(HttpHost("proxy.example.com"))
		val httpClient: HttpClient = HttpClientBuilder.create().setRoutePlanner(routePlanner).build()
		restTemplate.requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)
	}

	internal class CustomRoutePlanner(proxy: HttpHost?) : DefaultProxyRoutePlanner(proxy) {

		@Throws(HttpException::class)
		public override fun determineProxy(target: HttpHost, context: HttpContext): HttpHost? {
			if (target.hostName == "192.168.0.5") {
				return null
			}
			return  super.determineProxy(target, context)
		}

	}

}

