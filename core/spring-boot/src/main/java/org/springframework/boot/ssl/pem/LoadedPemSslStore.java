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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.ThrowingSupplier;

/**
 * {@link PemSslStore} loaded from {@link PemSslStoreDetails}.
 *
 * @author Phillip Webb
 * @see PemSslStore#load(PemSslStoreDetails)
 */
final class LoadedPemSslStore implements PemSslStore {

	private final PemSslStoreDetails details;

	private final ResourceLoader resourceLoader;

	private final Supplier<CertificatesHolder> certificatesSupplier;

	private final Supplier<PrivateKeyHolder> privateKeySupplier;

	LoadedPemSslStore(PemSslStoreDetails details, ResourceLoader resourceLoader) {
		Assert.notNull(details, "'details' must not be null");
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		this.details = details;
		this.resourceLoader = resourceLoader;
		this.certificatesSupplier = supplier(() -> loadCertificates(details, resourceLoader));
		this.privateKeySupplier = supplier(() -> loadPrivateKey(details, resourceLoader));
	}

	private static <T> Supplier<T> supplier(ThrowingSupplier<T> supplier) {
		return SingletonSupplier.of(supplier.throwing(LoadedPemSslStore::asUncheckedIOException));
	}

	private static UncheckedIOException asUncheckedIOException(String message, Exception cause) {
		return new UncheckedIOException(message, (IOException) cause);
	}

	private static CertificatesHolder loadCertificates(PemSslStoreDetails details, ResourceLoader resourceLoader)
			throws IOException {
		PemContent pemContent = PemContent.load(details.certificates(), resourceLoader);
		if (pemContent == null) {
			return new CertificatesHolder(null);
		}
		List<X509Certificate> certificates = pemContent.getCertificates();
		Assert.state(!CollectionUtils.isEmpty(certificates), "Loaded certificates are empty");
		return new CertificatesHolder(certificates);
	}

	private static PrivateKeyHolder loadPrivateKey(PemSslStoreDetails details, ResourceLoader resourceLoader)
			throws IOException {
		PemContent pemContent = PemContent.load(details.privateKey(), resourceLoader);
		return new PrivateKeyHolder(
				(pemContent != null) ? pemContent.getPrivateKey(details.privateKeyPassword()) : null);
	}

	@Override
	public @Nullable String type() {
		return this.details.type();
	}

	@Override
	public @Nullable String alias() {
		return this.details.alias();
	}

	@Override
	public @Nullable String password() {
		return this.details.password();
	}

	@Override
	public @Nullable List<X509Certificate> certificates() {
		return this.certificatesSupplier.get().certificates();
	}

	@Override
	public @Nullable PrivateKey privateKey() {
		return this.privateKeySupplier.get().privateKey();
	}

	@Override
	public PemSslStore withAlias(@Nullable String alias) {
		return new LoadedPemSslStore(this.details.withAlias(alias), this.resourceLoader);
	}

	@Override
	public PemSslStore withPassword(@Nullable String password) {
		return new LoadedPemSslStore(this.details.withPassword(password), this.resourceLoader);
	}

	private record PrivateKeyHolder(@Nullable PrivateKey privateKey) {
	}

	private record CertificatesHolder(@Nullable List<X509Certificate> certificates) {
	}

}
