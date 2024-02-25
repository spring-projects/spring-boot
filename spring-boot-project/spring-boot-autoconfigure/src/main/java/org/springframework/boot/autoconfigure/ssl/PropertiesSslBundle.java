/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

import org.springframework.boot.autoconfigure.ssl.SslBundleProperties.Key;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStore;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * {@link SslBundle} backed by {@link JksSslBundleProperties} or
 * {@link PemSslBundleProperties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public final class PropertiesSslBundle implements SslBundle {

	private final SslStoreBundle stores;

	private final SslBundleKey key;

	private final SslOptions options;

	private final String protocol;

	private final SslManagerBundle managers;

	/**
	 * Constructs a new PropertiesSslBundle with the specified SslStoreBundle and
	 * SslBundleProperties.
	 * @param stores the SslStoreBundle containing the SSL stores
	 * @param properties the SslBundleProperties containing the SSL bundle properties
	 */
	private PropertiesSslBundle(SslStoreBundle stores, SslBundleProperties properties) {
		this.stores = stores;
		this.key = asSslKeyReference(properties.getKey());
		this.options = asSslOptions(properties.getOptions());
		this.protocol = properties.getProtocol();
		this.managers = SslManagerBundle.from(this.stores, this.key);
	}

	/**
	 * Converts a Key object to an SslBundleKey object.
	 * @param key the Key object to be converted
	 * @return the corresponding SslBundleKey object, or SslBundleKey.NONE if the input
	 * key is null
	 */
	private static SslBundleKey asSslKeyReference(Key key) {
		return (key != null) ? SslBundleKey.of(key.getPassword(), key.getAlias()) : SslBundleKey.NONE;
	}

	/**
	 * Converts the given {@link SslBundleProperties.Options} object to an
	 * {@link SslOptions} object.
	 * @param options the {@link SslBundleProperties.Options} object to convert
	 * @return the converted {@link SslOptions} object
	 */
	private static SslOptions asSslOptions(SslBundleProperties.Options options) {
		return (options != null) ? SslOptions.of(options.getCiphers(), options.getEnabledProtocols()) : SslOptions.NONE;
	}

	/**
	 * Returns the SSL store bundle.
	 * @return the SSL store bundle
	 */
	@Override
	public SslStoreBundle getStores() {
		return this.stores;
	}

	/**
	 * Returns the SSL bundle key.
	 * @return the SSL bundle key
	 */
	@Override
	public SslBundleKey getKey() {
		return this.key;
	}

	/**
	 * Returns the SSL options associated with this PropertiesSslBundle.
	 * @return the SSL options
	 */
	@Override
	public SslOptions getOptions() {
		return this.options;
	}

	/**
	 * Returns the protocol used by the SSL bundle.
	 * @return the protocol used by the SSL bundle
	 */
	@Override
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Returns the SslManagerBundle object associated with this PropertiesSslBundle.
	 * @return the SslManagerBundle object associated with this PropertiesSslBundle
	 */
	@Override
	public SslManagerBundle getManagers() {
		return this.managers;
	}

	/**
	 * Get an {@link SslBundle} for the given {@link PemSslBundleProperties}.
	 * @param properties the source properties
	 * @return an {@link SslBundle} instance
	 */
	public static SslBundle get(PemSslBundleProperties properties) {
		PemSslStore keyStore = getPemSslStore("keystore", properties.getKeystore());
		if (keyStore != null) {
			keyStore = keyStore.withAlias(properties.getKey().getAlias())
				.withPassword(properties.getKey().getPassword());
		}
		PemSslStore trustStore = getPemSslStore("truststore", properties.getTruststore());
		SslStoreBundle storeBundle = new PemSslStoreBundle(keyStore, trustStore);
		return new PropertiesSslBundle(storeBundle, properties);
	}

	/**
	 * Retrieves the PEM SSL store based on the given property name and properties.
	 * @param propertyName the name of the property
	 * @param properties the PEM SSL bundle properties
	 * @return the PEM SSL store
	 */
	private static PemSslStore getPemSslStore(String propertyName, PemSslBundleProperties.Store properties) {
		PemSslStore pemSslStore = PemSslStore.load(asPemSslStoreDetails(properties));
		if (properties.isVerifyKeys()) {
			CertificateMatcher certificateMatcher = new CertificateMatcher(pemSslStore.privateKey());
			Assert.state(certificateMatcher.matchesAny(pemSslStore.certificates()),
					"Private key in %s matches none of the certificates in the chain".formatted(propertyName));
		}
		return pemSslStore;
	}

	/**
	 * Converts a PemSslBundleProperties.Store object to a PemSslStoreDetails object.
	 * @param properties the PemSslBundleProperties.Store object to convert
	 * @return the converted PemSslStoreDetails object
	 */
	private static PemSslStoreDetails asPemSslStoreDetails(PemSslBundleProperties.Store properties) {
		return new PemSslStoreDetails(properties.getType(), properties.getCertificate(), properties.getPrivateKey(),
				properties.getPrivateKeyPassword());
	}

	/**
	 * Get an {@link SslBundle} for the given {@link JksSslBundleProperties}.
	 * @param properties the source properties
	 * @return an {@link SslBundle} instance
	 */
	public static SslBundle get(JksSslBundleProperties properties) {
		SslStoreBundle storeBundle = asSslStoreBundle(properties);
		return new PropertiesSslBundle(storeBundle, properties);
	}

	/**
	 * Converts the given JksSslBundleProperties object into an SslStoreBundle object.
	 * @param properties the JksSslBundleProperties object to convert
	 * @return the converted SslStoreBundle object
	 */
	private static SslStoreBundle asSslStoreBundle(JksSslBundleProperties properties) {
		JksSslStoreDetails keyStoreDetails = asStoreDetails(properties.getKeystore());
		JksSslStoreDetails trustStoreDetails = asStoreDetails(properties.getTruststore());
		return new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
	}

	/**
	 * Converts the given JksSslBundleProperties.Store object to a JksSslStoreDetails
	 * object.
	 * @param properties the JksSslBundleProperties.Store object to convert
	 * @return the converted JksSslStoreDetails object
	 */
	private static JksSslStoreDetails asStoreDetails(JksSslBundleProperties.Store properties) {
		return new JksSslStoreDetails(properties.getType(), properties.getProvider(), properties.getLocation(),
				properties.getPassword());
	}

	/**
	 * Returns a string representation of the PropertiesSslBundle object.
	 * @return a string representation of the object
	 */
	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("key", this.key);
		creator.append("options", this.options);
		creator.append("protocol", this.protocol);
		creator.append("stores", this.stores);
		return creator.toString();
	}

}
