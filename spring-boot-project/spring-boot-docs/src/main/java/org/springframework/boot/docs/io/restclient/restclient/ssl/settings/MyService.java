/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docs.io.restclient.restclient.ssl.settings;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MyService {

	private final RestClient restClient;

	public MyService(RestClient.Builder restClientBuilder, SslBundles sslBundles) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings
			.ofSslBundle(sslBundles.getBundle("mybundle"))
			.withReadTimeout(Duration.ofMinutes(2));
		ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);
		this.restClient = restClientBuilder.baseUrl("https://example.org").requestFactory(requestFactory).build();
	}

	public Details someRestCall(String name) {
		return this.restClient.get().uri("/{name}/details", name).retrieve().body(Details.class);
	}

}
