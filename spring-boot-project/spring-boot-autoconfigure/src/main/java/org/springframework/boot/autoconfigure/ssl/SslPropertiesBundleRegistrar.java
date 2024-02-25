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

package org.springframework.boot.autoconfigure.ssl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SslPropertiesBundleRegistrar implements SslBundleRegistrar {

	private final SslProperties.Bundles properties;

	private final FileWatcher fileWatcher;

	/**
	 * Constructs a new SslPropertiesBundleRegistrar with the specified SslProperties and
	 * FileWatcher.
	 * @param properties the SslProperties object containing the SSL properties bundle
	 * @param fileWatcher the FileWatcher object used to monitor changes in the SSL
	 * properties file
	 */
	SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher) {
		this.properties = properties.getBundle();
		this.fileWatcher = fileWatcher;
	}

	/**
	 * Registers SSL bundles with the given SSL bundle registry.
	 * @param registry the SSL bundle registry to register the bundles with
	 */
	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::watchedPemPaths);
		registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::watchedJksPaths);
	}

	/**
	 * Registers SSL bundles in the given registry using the provided properties.
	 * @param registry the SSL bundle registry to register the bundles in
	 * @param properties a map of bundle names to their corresponding properties
	 * @param bundleFactory a function that creates an SSL bundle from its properties
	 * @param watchedPaths a function that returns the set of paths to be watched for
	 * updates for a given bundle
	 * @param <P> the type of the SSL bundle properties
	 * @throws IllegalStateException if unable to register an SSL bundle
	 */
	private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
			Function<P, SslBundle> bundleFactory, Function<P, Set<Path>> watchedPaths) {
		properties.forEach((bundleName, bundleProperties) -> {
			Supplier<SslBundle> bundleSupplier = () -> bundleFactory.apply(bundleProperties);
			try {
				registry.registerBundle(bundleName, bundleSupplier.get());
				if (bundleProperties.isReloadOnUpdate()) {
					Supplier<Set<Path>> pathsSupplier = () -> watchedPaths.apply(bundleProperties);
					watchForUpdates(registry, bundleName, pathsSupplier, bundleSupplier);
				}
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException("Unable to register SSL bundle '%s'".formatted(bundleName), ex);
			}
		});
	}

	/**
	 * Watches for updates on the specified paths and triggers the update of the SSL
	 * bundle in the registry.
	 * @param registry the SSL bundle registry
	 * @param bundleName the name of the SSL bundle
	 * @param pathsSupplier a supplier that provides the set of paths to watch for updates
	 * @param bundleSupplier a supplier that provides the SSL bundle to update
	 * @throws IllegalStateException if unable to watch for reload on update
	 */
	private void watchForUpdates(SslBundleRegistry registry, String bundleName, Supplier<Set<Path>> pathsSupplier,
			Supplier<SslBundle> bundleSupplier) {
		try {
			this.fileWatcher.watch(pathsSupplier.get(), () -> registry.updateBundle(bundleName, bundleSupplier.get()));
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to watch for reload on update", ex);
		}
	}

	/**
	 * Returns a set of watched paths for the specified JksSslBundleProperties.
	 * @param properties the JksSslBundleProperties containing the keystore and truststore
	 * locations
	 * @return a set of watched paths for the specified JksSslBundleProperties
	 */
	private Set<Path> watchedJksPaths(JksSslBundleProperties properties) {
		List<BundleContentProperty> watched = new ArrayList<>();
		watched.add(new BundleContentProperty("keystore.location", properties.getKeystore().getLocation()));
		watched.add(new BundleContentProperty("truststore.location", properties.getTruststore().getLocation()));
		return watchedPaths(watched);
	}

	/**
	 * Returns a set of paths to be watched for changes based on the provided
	 * PemSslBundleProperties. The paths include the private key and certificate paths for
	 * both the keystore and truststore.
	 * @param properties the PemSslBundleProperties containing the keystore and truststore
	 * information
	 * @return a set of paths to be watched for changes
	 */
	private Set<Path> watchedPemPaths(PemSslBundleProperties properties) {
		List<BundleContentProperty> watched = new ArrayList<>();
		watched.add(new BundleContentProperty("keystore.private-key", properties.getKeystore().getPrivateKey()));
		watched.add(new BundleContentProperty("keystore.certificate", properties.getKeystore().getCertificate()));
		watched.add(new BundleContentProperty("truststore.private-key", properties.getTruststore().getPrivateKey()));
		watched.add(new BundleContentProperty("truststore.certificate", properties.getTruststore().getCertificate()));
		return watchedPaths(watched);
	}

	/**
	 * Returns a set of watched paths based on the provided list of bundle content
	 * properties. Only properties with a non-null value are considered. Each property is
	 * mapped to its corresponding watch path. The resulting set contains unique watch
	 * paths.
	 * @param properties the list of bundle content properties to process
	 * @return a set of watched paths
	 */
	private Set<Path> watchedPaths(List<BundleContentProperty> properties) {
		return properties.stream()
			.filter(BundleContentProperty::hasValue)
			.map(BundleContentProperty::toWatchPath)
			.collect(Collectors.toSet());
	}

}
