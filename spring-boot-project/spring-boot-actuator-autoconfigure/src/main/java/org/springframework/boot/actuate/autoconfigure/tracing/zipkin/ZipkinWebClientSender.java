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

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A Zipkin {@link HttpSender} which uses {@link WebClient} for HTTP communication.
 *
 * @author Stefan Bratanov
 */
class ZipkinWebClientSender extends HttpSender {

	private final String endpoint;

	private final WebClient webClient;

	ZipkinWebClientSender(String endpoint, WebClient webClient) {
		this.endpoint = endpoint;
		this.webClient = webClient;
	}

	@Override
	public HttpPostCall sendSpans(byte[] batchedEncodedSpans) {
		return new WebClientHttpPostCall(this.endpoint, batchedEncodedSpans, this.webClient);
	}

	private static class WebClientHttpPostCall extends HttpPostCall {

		private final String endpoint;

		private final WebClient webClient;

		WebClientHttpPostCall(String endpoint, byte[] body, WebClient webClient) {
			super(body);
			this.endpoint = endpoint;
			this.webClient = webClient;
		}

		@Override
		void post(byte[] body, HttpHeaders defaultHeaders) {
			this.webClient.post().uri(this.endpoint).headers((headers) -> headers.addAll(defaultHeaders))
					.bodyValue(body).retrieve().toBodilessEntity().subscribe((__) -> {
					}, (__) -> {
					});
		}

		@Override
		public Call<Void> clone() {
			return new WebClientHttpPostCall(this.endpoint, this.body, this.webClient);
		}

	}

}
