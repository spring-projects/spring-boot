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

import java.security.KeyStore;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;

/**
 * An {@link SslStoreProvider} that creates key and trust stores from certificate and
 * private key PEM files.
 *
 * @author Scott Frederick
 * @since 2.7.0
 * @deprecated since 3.1.0 for removal in 3.3.0 in favor of registering a
 * {@link SslBundle} backed by a {@link PemSslStoreBundle}.
 */
@Deprecated(since = "3.1.0", forRemoval = true)
@SuppressWarnings({ "deprecation", "removal" })
public final class CertificateFileSslStoreProvider implements SslStoreProvider {

	private final SslBundle delegate;

	private CertificateFileSslStoreProvider(SslBundle delegate) {
		this.delegate = delegate;
	}

	@Override
	public KeyStore getKeyStore() throws Exception {
		return this.delegate.getStores().getKeyStore();
	}

	@Override
	public KeyStore getTrustStore() throws Exception {
		return this.delegate.getStores().getTrustStore();
	}

	@Override
	public String getKeyPassword() {
		return this.delegate.getKey().getPassword();
	}

	/**
	 * Create an {@link SslStoreProvider} if the appropriate SSL properties are
	 * configured.
	 * @param ssl the SSL properties
	 * @return an {@code SslStoreProvider} or {@code null}
	 */
	public static SslStoreProvider from(Ssl ssl) {
		SslBundle delegate = WebServerSslBundle.createCertificateFileSslStoreProviderDelegate(ssl);
		return (delegate != null) ? new CertificateFileSslStoreProvider(delegate) : null;
	}

}
