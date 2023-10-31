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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link PemSslStore} loaded from {@link PemSslStoreDetails}.
 *
 * @author Phillip Webb
 * @see PemSslStore#load(PemSslStoreDetails)
 */
final class LoadedPemSslStore implements PemSslStore {

	private final PemSslStoreDetails details;

	private final List<X509Certificate> certificates;

	private final PrivateKey privateKey;

	LoadedPemSslStore(PemSslStoreDetails details) throws IOException {
		Assert.notNull(details, "Details must not be null");
		this.details = details;
		this.certificates = loadCertificates(details);
		this.privateKey = loadPrivateKey(details);
	}

	private static List<X509Certificate> loadCertificates(PemSslStoreDetails details) throws IOException {
		PemContent pemContent = PemContent.load(details.certificates());
		if (pemContent == null) {
			return null;
		}
		List<X509Certificate> certificates = pemContent.getCertificates();
		Assert.state(!CollectionUtils.isEmpty(certificates), "Loaded certificates are empty");
		return certificates;
	}

	private static PrivateKey loadPrivateKey(PemSslStoreDetails details) throws IOException {
		PemContent pemContent = PemContent.load(details.privateKey());
		return (pemContent != null) ? pemContent.getPrivateKey(details.privateKeyPassword()) : null;
	}

	@Override
	public String type() {
		return this.details.type();
	}

	@Override
	public String alias() {
		return this.details.alias();
	}

	@Override
	public String password() {
		return this.details.password();
	}

	@Override
	public List<X509Certificate> certificates() {
		return this.certificates;
	}

	@Override
	public PrivateKey privateKey() {
		return this.privateKey;
	}

}
