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

package org.springframework.boot.web.server;

/**
 * Creates an {@link SslStoreProvider} based on SSL configuration properties.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public final class SslStoreProviderFactory {

	private SslStoreProviderFactory() {
	}

	/**
	 * Create an {@link SslStoreProvider} if the appropriate SSL properties are
	 * configured.
	 * @param ssl the SSL properties
	 * @return an {@code SslStoreProvider} or {@code null}
	 */
	public static SslStoreProvider from(Ssl ssl) {
		SslStoreProvider sslStoreProvider = CertificateFileSslStoreProvider.from(ssl);
		return ((sslStoreProvider != null) ? sslStoreProvider : JavaKeyStoreSslStoreProvider.from(ssl));
	}

}
