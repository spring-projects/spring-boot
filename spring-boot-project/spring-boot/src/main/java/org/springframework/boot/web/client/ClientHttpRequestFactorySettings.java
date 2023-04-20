/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.client;

import java.time.Duration;

import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Settings that can be applied when creating a {@link ClientHttpRequestFactory}.
 *
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param bufferRequestBody if request body buffering is used
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 * @see ClientHttpRequestFactories
 */
public record ClientHttpRequestFactorySettings(Duration connectTimeout, Duration readTimeout,
		Boolean bufferRequestBody) {

	/**
	 * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
	 * the implementation.
	 */
	public static final ClientHttpRequestFactorySettings DEFAULTS = new ClientHttpRequestFactorySettings(null, null,
			null);

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * connect timeout setting .
	 * @param connectTimeout the new connect timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withConnectTimeout(Duration connectTimeout) {
		return new ClientHttpRequestFactorySettings(connectTimeout, this.readTimeout, this.bufferRequestBody);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated read
	 * timeout setting.
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */

	public ClientHttpRequestFactorySettings withReadTimeout(Duration readTimeout) {
		return new ClientHttpRequestFactorySettings(this.connectTimeout, readTimeout, this.bufferRequestBody);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * buffer request body setting.
	 * @param bufferRequestBody the new buffer request body setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withBufferRequestBody(Boolean bufferRequestBody) {
		return new ClientHttpRequestFactorySettings(this.connectTimeout, this.readTimeout, bufferRequestBody);
	}

}
