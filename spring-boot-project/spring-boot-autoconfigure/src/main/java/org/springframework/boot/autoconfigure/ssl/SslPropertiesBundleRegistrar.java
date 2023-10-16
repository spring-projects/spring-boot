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

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link SslBundleRegistrar} that registers SSL bundles based
 * {@link SslProperties#getBundle() configuration properties}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class SslPropertiesBundleRegistrar implements SslBundleRegistrar {

	private static final Pattern PEM_CONTENT = Pattern.compile("-+BEGIN\\s+[^-]*-+", Pattern.CASE_INSENSITIVE);

	private final SslProperties.Bundles properties;

	private final FileWatcher fileWatcher;

	SslPropertiesBundleRegistrar(SslProperties properties, FileWatcher fileWatcher) {
		this.properties = properties.getBundle();
		this.fileWatcher = fileWatcher;
	}

	@Override
	public void registerBundles(SslBundleRegistry registry) {
		registerBundles(registry, this.properties.getPem(), PropertiesSslBundle::get, this::getLocations);
		registerBundles(registry, this.properties.getJks(), PropertiesSslBundle::get, this::getLocations);
	}

	private <P extends SslBundleProperties> void registerBundles(SslBundleRegistry registry, Map<String, P> properties,
			Function<P, SslBundle> bundleFactory, Function<P, Set<Location>> locationsSupplier) {
		properties.forEach((bundleName, bundleProperties) -> {
			SslBundle bundle = bundleFactory.apply(bundleProperties);
			registry.registerBundle(bundleName, bundle);
			if (bundleProperties.isReloadOnUpdate()) {
				Set<Path> paths = locationsSupplier.apply(bundleProperties)
					.stream()
					.filter(Location::hasValue)
					.map((location) -> toPath(bundleName, location))
					.collect(Collectors.toSet());
				this.fileWatcher.watch(paths,
						() -> registry.updateBundle(bundleName, bundleFactory.apply(bundleProperties)));
			}
		});
	}

	private Set<Location> getLocations(JksSslBundleProperties properties) {
		JksSslBundleProperties.Store keystore = properties.getKeystore();
		JksSslBundleProperties.Store truststore = properties.getTruststore();
		Set<Location> locations = new LinkedHashSet<>();
		locations.add(new Location("keystore.location", keystore.getLocation()));
		locations.add(new Location("truststore.location", truststore.getLocation()));
		return locations;
	}

	private Set<Location> getLocations(PemSslBundleProperties properties) {
		PemSslBundleProperties.Store keystore = properties.getKeystore();
		PemSslBundleProperties.Store truststore = properties.getTruststore();
		Set<Location> locations = new LinkedHashSet<>();
		locations.add(new Location("keystore.private-key", keystore.getPrivateKey()));
		locations.add(new Location("keystore.certificate", keystore.getCertificate()));
		locations.add(new Location("truststore.private-key", truststore.getPrivateKey()));
		locations.add(new Location("truststore.certificate", truststore.getCertificate()));
		return locations;
	}

	private Path toPath(String bundleName, Location watchableLocation) {
		String value = watchableLocation.value();
		String field = watchableLocation.field();
		Assert.state(!PEM_CONTENT.matcher(value).find(),
				() -> "SSL bundle '%s' '%s' is not a URL and can't be watched".formatted(bundleName, field));
		try {
			URL url = ResourceUtils.getURL(value);
			Assert.state("file".equalsIgnoreCase(url.getProtocol()),
					() -> "SSL bundle '%s' '%s' URL '%s' doesn't point to a file".formatted(bundleName, field, url));
			return Path.of(url.getFile()).toAbsolutePath();
		}
		catch (FileNotFoundException ex) {
			throw new UncheckedIOException(
					"SSL bundle '%s' '%s' location '%s' cannot be watched".formatted(bundleName, field, value), ex);
		}
	}

	private record Location(String field, String value) {

		boolean hasValue() {
			return StringUtils.hasText(this.value);
		}

	}

}
