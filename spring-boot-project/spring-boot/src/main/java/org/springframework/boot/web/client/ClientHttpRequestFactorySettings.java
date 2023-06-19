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

import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.ClientHttpRequestFactory;

/**
 * Settings that can be applied when creating a {@link ClientHttpRequestFactory}.
 *
 * @param connectTimeout the connect timeout
 * @param readTimeout the read timeout
 * @param sslBundle the SSL bundle providing SSL configuration
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 3.0.0
 * @see ClientHttpRequestFactories
 */
public record ClientHttpRequestFactorySettings(Duration connectTimeout, Duration readTimeout, SslBundle sslBundle) {

	/**
	 * Use defaults for the {@link ClientHttpRequestFactory} which can differ depending on
	 * the implementation.
	 */
	public static final ClientHttpRequestFactorySettings DEFAULTS = new ClientHttpRequestFactorySettings(null, null,
			null, null);

	/**
	 * Create a new {@link ClientHttpRequestFactorySettings} instance.
	 * @param connectTimeout the connection timeout
	 * @param readTimeout the read timeout
	 * @param bufferRequestBody if request body buffering is used
	 * @deprecated since 3.2.0 for removal in 3.4.0 as support for buffering has been
	 * removed in Spring Framework 6.1
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ClientHttpRequestFactorySettings(Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody) {
		this(connectTimeout, readTimeout, (SslBundle) null);
	}

	/**
	 * Create a new {@link ClientHttpRequestFactorySettings} instance.
	 * @param connectTimeout the connection timeout
	 * @param readTimeout the read timeout
	 * @param bufferRequestBody if request body buffering is used
	 * @param sslBundle the ssl bundle
	 * @since 3.1.0
	 * @deprecated since 3.2.0 for removal in 3.4.0 as support for buffering has been
	 * removed in Spring Framework 6.1
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ClientHttpRequestFactorySettings(Duration connectTimeout, Duration readTimeout, Boolean bufferRequestBody,
			SslBundle sslBundle) {
		this(connectTimeout, readTimeout, sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated
	 * connect timeout setting .
	 * @param connectTimeout the new connect timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */
	public ClientHttpRequestFactorySettings withConnectTimeout(Duration connectTimeout) {
		return new ClientHttpRequestFactorySettings(connectTimeout, this.readTimeout, this.sslBundle);
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated read
	 * timeout setting.
	 * @param readTimeout the new read timeout setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 */

	public ClientHttpRequestFactorySettings withReadTimeout(Duration readTimeout) {
		return new ClientHttpRequestFactorySettings(this.connectTimeout, readTimeout, this.sslBundle);
	}

	/**
	 * Has no effect as support for buffering has been removed in Spring Framework 6.1.
	 * @param bufferRequestBody the new buffer request body setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 * @deprecated since 3.2.0 for removal in 3.4.0 as support for buffering has been
	 * removed in Spring Framework 6.1
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public ClientHttpRequestFactorySettings withBufferRequestBody(Boolean bufferRequestBody) {
		return this;
	}

	/**
	 * Return a new {@link ClientHttpRequestFactorySettings} instance with an updated SSL
	 * bundle setting.
	 * @param sslBundle the new SSL bundle setting
	 * @return a new {@link ClientHttpRequestFactorySettings} instance
	 * @since 3.1.0
	 */
	public ClientHttpRequestFactorySettings withSslBundle(SslBundle sslBundle) {
		return new ClientHttpRequestFactorySettings(this.connectTimeout, this.readTimeout, sslBundle);
	}

	/**
	 * Returns whether request body buffering is used.
	 * @return whether request body buffering is used
	 * @deprecated since 3.2.0 for removal in 3.4.0 as support for buffering has been
	 * removed in Spring Framework 6.1
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public Boolean bufferRequestBody() {
		return null;
	}

}
