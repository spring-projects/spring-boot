/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.restclient.ssl;

import org.springframework.boot.restclient.autoconfigure.RestClientSsl;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MyService {

	private final RestClient restClient;

	public MyService(RestClient.Builder restClientBuilder, RestClientSsl ssl) {
		this.restClient = restClientBuilder.baseUrl("https://example.org").apply(ssl.fromBundle("mybundle")).build();
	}

	public Details someRestCall(String name) {
		return this.restClient.get().uri("/{name}/details", name).retrieve().body(Details.class);
	}

}
