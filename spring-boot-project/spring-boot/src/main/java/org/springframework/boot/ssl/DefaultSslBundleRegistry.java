/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * Default {@link SslBundleRegistry} implementation.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author Jonatan Ivanov
 * @since 3.1.0
 */
public class DefaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

	private static final Log logger = LogFactory.getLog(DefaultSslBundleRegistry.class);

	private final Map<String, RegisteredSslBundle> registeredBundles = new ConcurrentHashMap<>();

	private final List<BiConsumer<String, SslBundle>> registerHandlers = new CopyOnWriteArrayList<>();

	public DefaultSslBundleRegistry() {
	}

	public DefaultSslBundleRegistry(String name, SslBundle bundle) {
		registerBundle(name, bundle);
	}

	@Override
	public void registerBundle(String name, SslBundle bundle) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(bundle, "'bundle' must not be null");
		RegisteredSslBundle previous = this.registeredBundles.putIfAbsent(name, new RegisteredSslBundle(name, bundle));
		Assert.state(previous == null, () -> "Cannot replace existing SSL bundle '%s'".formatted(name));
		this.registerHandlers.forEach((handler) -> handler.accept(name, bundle));
	}

	@Override
	public void updateBundle(String name, SslBundle updatedBundle) {
		getRegistered(name).update(updatedBundle);
	}

	@Override
	public SslBundle getBundle(String name) {
		return getRegistered(name).getBundle();
	}

	@Override
	public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException {
		getRegistered(name).addUpdateHandler(updateHandler);
	}

	@Override
	public void addBundleRegisterHandler(BiConsumer<String, SslBundle> registerHandler) {
		this.registerHandlers.add(registerHandler);
	}

	@Override
	public List<String> getBundleNames() {
		List<String> names = new ArrayList<>(this.registeredBundles.keySet());
		Collections.sort(names);
		return Collections.unmodifiableList(names);
	}

	private RegisteredSslBundle getRegistered(String name) throws NoSuchSslBundleException {
		Assert.notNull(name, "'name' must not be null");
		RegisteredSslBundle registered = this.registeredBundles.get(name);
		if (registered == null) {
			throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
		}
		return registered;
	}

	private static class RegisteredSslBundle {

		private final String name;

		private final List<Consumer<SslBundle>> updateHandlers = new CopyOnWriteArrayList<>();

		private volatile SslBundle bundle;

		RegisteredSslBundle(String name, SslBundle bundle) {
			this.name = name;
			this.bundle = bundle;
		}

		void update(SslBundle updatedBundle) {
			Assert.notNull(updatedBundle, "'updatedBundle' must not be null");
			this.bundle = updatedBundle;
			if (this.updateHandlers.isEmpty()) {
				logger.warn(LogMessage.format(
						"SSL bundle '%s' has been updated but may be in use by a technology that doesn't support SSL reloading",
						this.name));
			}
			this.updateHandlers.forEach((handler) -> handler.accept(updatedBundle));
		}

		SslBundle getBundle() {
			return this.bundle;
		}

		void addUpdateHandler(Consumer<SslBundle> updateHandler) {
			Assert.notNull(updateHandler, "'updateHandler' must not be null");
			this.updateHandlers.add(updateHandler);
		}

	}

}
