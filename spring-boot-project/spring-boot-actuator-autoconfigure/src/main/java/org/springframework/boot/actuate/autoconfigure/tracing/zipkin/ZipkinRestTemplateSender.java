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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import zipkin2.Call;
import zipkin2.Callback;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * A {@link HttpSender} which uses {@link RestTemplate} for HTTP communication.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 */
class ZipkinRestTemplateSender extends HttpSender {

	private final String endpoint;

	private final RestTemplate restTemplate;

	ZipkinRestTemplateSender(String endpoint, RestTemplate restTemplate) {
		this.endpoint = endpoint;
		this.restTemplate = restTemplate;
	}

	@Override
	public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
		return new RestTemplateHttpPostCall(this.endpoint, batchedEncodedSpans, this.restTemplate);
	}

	private static class RestTemplateHttpPostCall extends HttpPostCall {

		private final String endpoint;

		private final RestTemplate restTemplate;

		RestTemplateHttpPostCall(String endpoint, byte[] body, RestTemplate restTemplate) {
			super(body);
			this.endpoint = endpoint;
			this.restTemplate = restTemplate;
		}

		@Override
		public Call<Void> clone() {
			return new RestTemplateHttpPostCall(this.endpoint, getUncompressedBody(), this.restTemplate);
		}

		@Override
		protected Void doExecute() {
			HttpEntity<byte[]> request = new HttpEntity<>(getBody(), getDefaultHeaders());
			this.restTemplate.exchange(this.endpoint, HttpMethod.POST, request, Void.class);
			return null;
		}

		@Override
		protected void doEnqueue(Callback<Void> callback) {
			try {
				doExecute();
				callback.onSuccess(null);
			}
			catch (Exception ex) {
				callback.onError(ex);
			}
		}

	}

}
