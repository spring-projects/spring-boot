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
import java.io.UncheckedIOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

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

	private final Supplier<List<X509Certificate>> certificatesSupplier;

	private final Supplier<PrivateKey> privateKeySupplier;

	/**
	 * Constructs a new LoadedPemSslStore with the specified PemSslStoreDetails.
	 * @param details the PemSslStoreDetails containing the necessary information to load
	 * the SSL store
	 * @throws IllegalArgumentException if the details parameter is null
	 */
	LoadedPemSslStore(PemSslStoreDetails details) {
		Assert.notNull(details, "Details must not be null");
		this.details = details;
		this.certificatesSupplier = supplier(() -> loadCertificates(details));
		this.privateKeySupplier = supplier(() -> loadPrivateKey(details));
	}

	/**
	 * Returns a supplier that wraps the given ThrowingSupplier and handles any checked
	 * exceptions by converting them to UncheckedIOExceptions.
	 * @param supplier the ThrowingSupplier to be wrapped
	 * @return a Supplier that wraps the given ThrowingSupplier
	 * @throws NullPointerException if the supplier is null
	 */
	private static <T> Supplier<T> supplier(ThrowingSupplier<T> supplier) {
		return SingletonSupplier.of(supplier.throwing(LoadedPemSslStore::asUncheckedIOException));
	}

	/**
	 * Converts an exception to an unchecked IO exception with a specified message.
	 * @param message the detail message for the exception
	 * @param cause the exception to be converted
	 * @return an unchecked IO exception with the specified message and cause
	 */
	private static UncheckedIOException asUncheckedIOException(String message, Exception cause) {
		return new UncheckedIOException(message, (IOException) cause);
	}

	/**
	 * Loads the X509 certificates from the provided PemSslStoreDetails.
	 * @param details the PemSslStoreDetails containing the certificates
	 * @return a List of X509Certificates loaded from the PemSslStoreDetails
	 * @throws IOException if an I/O error occurs while loading the certificates
	 * @throws IllegalArgumentException if the provided PemSslStoreDetails is null
	 * @throws IllegalStateException if the loaded certificates are empty
	 */
	private static List<X509Certificate> loadCertificates(PemSslStoreDetails details) throws IOException {
		PemContent pemContent = PemContent.load(details.certificates());
		if (pemContent == null) {
			return null;
		}
		List<X509Certificate> certificates = pemContent.getCertificates();
		Assert.state(!CollectionUtils.isEmpty(certificates), "Loaded certificates are empty");
		return certificates;
	}

	/**
	 * Loads a private key from a PEM SSL store details.
	 * @param details the PEM SSL store details containing the private key
	 * @return the loaded private key, or null if the private key could not be loaded
	 * @throws IOException if an I/O error occurs while loading the private key
	 */
	private static PrivateKey loadPrivateKey(PemSslStoreDetails details) throws IOException {
		PemContent pemContent = PemContent.load(details.privateKey());
		return (pemContent != null) ? pemContent.getPrivateKey(details.privateKeyPassword()) : null;
	}

	/**
	 * Returns the type of the SSL store.
	 * @return the type of the SSL store
	 */
	@Override
	public String type() {
		return this.details.type();
	}

	/**
	 * Returns the alias of the LoadedPemSslStore.
	 * @return the alias of the LoadedPemSslStore
	 */
	@Override
	public String alias() {
		return this.details.alias();
	}

	/**
	 * Returns the password for the loaded PEM SSL store.
	 * @return the password for the loaded PEM SSL store
	 */
	@Override
	public String password() {
		return this.details.password();
	}

	/**
	 * Returns a list of X509Certificates.
	 * @return the list of X509Certificates
	 */
	@Override
	public List<X509Certificate> certificates() {
		return this.certificatesSupplier.get();
	}

	/**
	 * Returns the private key associated with this LoadedPemSslStore.
	 * @return the private key
	 */
	@Override
	public PrivateKey privateKey() {
		return this.privateKeySupplier.get();
	}

}
