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

package org.springframework.boot.ssl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * Default {@link SslBundleRegistry} implementation.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class DefaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

	private final Map<String, SslBundle> bundles = new ConcurrentHashMap<>();

	public DefaultSslBundleRegistry() {
	}

	public DefaultSslBundleRegistry(String name, SslBundle bundle) {
		registerBundle(name, bundle);
	}

	@Override
	public void registerBundle(String name, SslBundle bundle) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(bundle, "Bundle must not be null");
		SslBundle previous = this.bundles.putIfAbsent(name, bundle);
		Assert.state(previous == null, () -> "Cannot replace existing SSL bundle '%s'".formatted(name));
	}

	@Override
	public SslBundle getBundle(String name) {
		Assert.notNull(name, "Name must not be null");
		SslBundle bundle = this.bundles.get(name);
		if (bundle == null) {
			throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
		}
		return bundle;
	}

}
