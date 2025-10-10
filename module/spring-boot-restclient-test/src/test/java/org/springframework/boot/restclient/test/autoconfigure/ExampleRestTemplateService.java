/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restclient.test.autoconfigure;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Example web client using {@code RestTemplate} with
 * {@link RestClientTest @RestClientTest} tests.
 *
 * @author Phillip Webb
 */
@Service
public class ExampleRestTemplateService {

	private final RestTemplate restTemplate;

	public ExampleRestTemplateService(RestTemplateBuilder builder) {
		this.restTemplate = builder.rootUri("https://example.com").build();
	}

	protected RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	public String test() {
		return this.restTemplate.getForEntity("/test", String.class).getBody();
	}

	public void testPostWithBody(String body) {
		this.restTemplate.postForObject("/test", body, String.class);
	}

}
