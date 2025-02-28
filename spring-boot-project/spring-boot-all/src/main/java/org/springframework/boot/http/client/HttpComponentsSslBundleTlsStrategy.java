/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.http.client;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;

/**
 * Adapts {@link SslBundle} to an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link DefaultClientTlsStrategy}.
 *
 * @author Phillip Webb
 */
final class HttpComponentsSslBundleTlsStrategy {

	private HttpComponentsSslBundleTlsStrategy() {
	}

	static DefaultClientTlsStrategy get(SslBundle sslBundle) {
		if (sslBundle == null) {
			return null;
		}
		SslOptions options = sslBundle.getOptions();
		SSLContext sslContext = sslBundle.createSslContext();
		return new DefaultClientTlsStrategy(sslContext, options.getEnabledProtocols(), options.getCiphers(), null,
				new DefaultHostnameVerifier());
	}

}
