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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
 * @since 3.1.0
 */
public class DefaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

	private static final Log logger = LogFactory.getLog(DefaultSslBundleRegistry.class);

	private final Map<String, RegisteredSslBundle> registeredBundles = new ConcurrentHashMap<>();

	/**
     * Constructs a new DefaultSslBundleRegistry.
     */
    public DefaultSslBundleRegistry() {
	}

	/**
     * Constructs a new DefaultSslBundleRegistry with the specified name and SslBundle.
     * 
     * @param name the name of the registry
     * @param bundle the SslBundle to be registered
     */
    public DefaultSslBundleRegistry(String name, SslBundle bundle) {
		registerBundle(name, bundle);
	}

	/**
     * Registers a new SSL bundle with the given name.
     * 
     * @param name   the name of the SSL bundle (must not be null)
     * @param bundle the SSL bundle to be registered (must not be null)
     * @throws IllegalArgumentException if either the name or the bundle is null
     * @throws IllegalStateException    if an SSL bundle with the same name already exists
     */
    @Override
	public void registerBundle(String name, SslBundle bundle) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(bundle, "Bundle must not be null");
		RegisteredSslBundle previous = this.registeredBundles.putIfAbsent(name, new RegisteredSslBundle(name, bundle));
		Assert.state(previous == null, () -> "Cannot replace existing SSL bundle '%s'".formatted(name));
	}

	/**
     * Updates the SSL bundle with the specified name in the registry.
     * 
     * @param name          the name of the SSL bundle to update
     * @param updatedBundle the updated SSL bundle
     */
    @Override
	public void updateBundle(String name, SslBundle updatedBundle) {
		getRegistered(name).update(updatedBundle);
	}

	/**
     * Retrieves the SSL bundle associated with the specified name.
     * 
     * @param name the name of the SSL bundle to retrieve
     * @return the SSL bundle associated with the specified name
     */
    @Override
	public SslBundle getBundle(String name) {
		return getRegistered(name).getBundle();
	}

	/**
     * Adds a bundle update handler for the specified SSL bundle.
     * 
     * @param name the name of the SSL bundle
     * @param updateHandler the update handler to be added
     * @throws NoSuchSslBundleException if the SSL bundle with the specified name does not exist
     */
    @Override
	public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException {
		getRegistered(name).addUpdateHandler(updateHandler);
	}

	/**
     * Retrieves the registered SSL bundle with the specified name.
     *
     * @param name the name of the SSL bundle to retrieve
     * @return the registered SSL bundle
     * @throws NoSuchSslBundleException if the SSL bundle with the specified name cannot be found
     * @throws IllegalArgumentException if the name is null
     */
    private RegisteredSslBundle getRegistered(String name) throws NoSuchSslBundleException {
		Assert.notNull(name, "Name must not be null");
		RegisteredSslBundle registered = this.registeredBundles.get(name);
		if (registered == null) {
			throw new NoSuchSslBundleException(name, "SSL bundle name '%s' cannot be found".formatted(name));
		}
		return registered;
	}

	/**
     * RegisteredSslBundle class.
     */
    private static class RegisteredSslBundle {

		private final String name;

		private final List<Consumer<SslBundle>> updateHandlers = new CopyOnWriteArrayList<>();

		private volatile SslBundle bundle;

		/**
         * Constructs a new RegisteredSslBundle with the specified name and bundle.
         * 
         * @param name the name of the SSL bundle
         * @param bundle the SSL bundle object
         */
        RegisteredSslBundle(String name, SslBundle bundle) {
			this.name = name;
			this.bundle = bundle;
		}

		/**
         * Updates the SSL bundle with the provided updated bundle.
         * 
         * @param updatedBundle the updated SSL bundle (must not be null)
         * 
         * @throws IllegalArgumentException if the updatedBundle is null
         */
        void update(SslBundle updatedBundle) {
			Assert.notNull(updatedBundle, "UpdatedBundle must not be null");
			this.bundle = updatedBundle;
			if (this.updateHandlers.isEmpty()) {
				logger.warn(LogMessage.format(
						"SSL bundle '%s' has been updated but may be in use by a technology that doesn't support SSL reloading",
						this.name));
			}
			this.updateHandlers.forEach((handler) -> handler.accept(updatedBundle));
		}

		/**
         * Returns the SSL bundle associated with this RegisteredSslBundle.
         *
         * @return the SSL bundle
         */
        SslBundle getBundle() {
			return this.bundle;
		}

		/**
         * Adds an update handler to the list of update handlers.
         * 
         * @param updateHandler the update handler to be added (must not be null)
         */
        void addUpdateHandler(Consumer<SslBundle> updateHandler) {
			Assert.notNull(updateHandler, "UpdateHandler must not be null");
			this.updateHandlers.add(updateHandler);
		}

	}

}
