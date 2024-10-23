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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.core.io.ResourceLoader;

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

	private final ResourceLoader resourceLoader;

	SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher, ResourceLoader resourceLoader) {
		this.properties = properties.getBundle();
		this.fileWatcher = fileWatcher;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::watchedPemPaths);
		registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::watchedJksPaths);
	}

	private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
			BiFunction<P, ResourceLoader, SslBundle> bundleFactory, Function<Bundle<P>, Set<Path>> watchedPaths) {
		properties.forEach((bundleName, bundleProperties) -> {
			Supplier<SslBundle> bundleSupplier = () -> bundleFactory.apply(bundleProperties, this.resourceLoader);
			try {
				registry.registerBundle(bundleName, bundleSupplier.get());
				if (bundleProperties.isReloadOnUpdate()) {
					Supplier<Set<Path>> pathsSupplier = () -> watchedPaths
						.apply(new Bundle<>(bundleName, bundleProperties));
					watchForUpdates(registry, bundleName, pathsSupplier, bundleSupplier);
				}
			}
			catch (IllegalStateException ex) {
				throw new IllegalStateException("Unable to register SSL bundle '%s'".formatted(bundleName), ex);
			}
		});
	}

	private void watchForUpdates(SslBundleRegistry registry, String bundleName, Supplier<Set<Path>> pathsSupplier,
			Supplier<SslBundle> bundleSupplier) {
		try {
			this.fileWatcher.watch(pathsSupplier.get(), () -> registry.updateBundle(bundleName, bundleSupplier.get()));
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("Unable to watch for reload on update", ex);
		}
	}

	private Set<Path> watchedJksPaths(Bundle<JksSslBundleProperties> bundle) {
		List<BundleContentProperty> watched = new ArrayList<>();
		watched.add(new BundleContentProperty("keystore.location", bundle.properties().getKeystore().getLocation()));
		watched
			.add(new BundleContentProperty("truststore.location", bundle.properties().getTruststore().getLocation()));
		return watchedPaths(bundle.name(), watched);
	}

	private Set<Path> watchedPemPaths(Bundle<PemSslBundleProperties> bundle) {
		List<BundleContentProperty> watched = new ArrayList<>();
		watched
			.add(new BundleContentProperty("keystore.private-key", bundle.properties().getKeystore().getPrivateKey()));
		watched
			.add(new BundleContentProperty("keystore.certificate", bundle.properties().getKeystore().getCertificate()));
		watched.add(new BundleContentProperty("truststore.private-key",
				bundle.properties().getTruststore().getPrivateKey()));
		watched.add(new BundleContentProperty("truststore.certificate",
				bundle.properties().getTruststore().getCertificate()));
		return watchedPaths(bundle.name(), watched);
	}

	private Set<Path> watchedPaths(String bundleName, List<BundleContentProperty> properties) {
		try {
			return properties.stream()
				.filter(BundleContentProperty::hasValue)
				.map((content) -> content.toWatchPath(this.resourceLoader))
				.collect(Collectors.toSet());
		}
		catch (BundleContentNotWatchableException ex) {
			throw ex.withBundleName(bundleName);
		}
	}

	private record Bundle<P>(String name, P properties) {
	}

}
