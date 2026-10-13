/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.resttemplate.ssl;

import org.springframework.boot.docs.io.restclient.resttemplate.Details;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MyService {

	private final RestTemplate restTemplate;

	public MyService(RestTemplateBuilder restTemplateBuilder, SslBundles sslBundles) {
		this.restTemplate = restTemplateBuilder.sslBundle(sslBundles.getBundle("mybundle")).build();
	}

	public Details someRestCall(String name) {
		return this.restTemplate.getForObject("/{name}/details", Details.class, name);
	}

}
