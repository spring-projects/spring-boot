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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.net.URI;

import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier.Factory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * An {@link HttpSender} which uses {@link RestTemplate} for HTTP communication.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 */
@Deprecated(since = "3.3.0", forRemoval = true)
class ZipkinRestTemplateSender extends HttpSender {

	private final RestTemplate restTemplate;

	ZipkinRestTemplateSender(Encoding encoding, Factory endpointSupplierFactory, String endpoint,
			RestTemplate restTemplate) {
		super(encoding, endpointSupplierFactory, endpoint);
		this.restTemplate = restTemplate;
	}

	@Override
	void postSpans(URI endpoint, HttpHeaders headers, byte[] body) {
		HttpEntity<byte[]> request = new HttpEntity<>(body, headers);
		this.restTemplate.exchange(endpoint, HttpMethod.POST, request, Void.class);
	}

}
