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

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A managed set of {@link SslBundle} instances that can be retrieved by name.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 * @since 3.1.0
 */
public interface SslBundles {

	/**
	 * Return an {@link SslBundle} with the provided name.
	 * @param name the bundle name
	 * @return the bundle
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 */
	SslBundle getBundle(String name) throws NoSuchSslBundleException;

	/**
	 * Add a handler that will be called each time the named bundle is updated.
	 * @param name the bundle name
	 * @param updateHandler the handler that should be called
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 * @since 3.2.0
	 */
	void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException;

	/**
	 * Add a handler that will be called each time a bundle is registered. The handler
	 * will be called with the bundle name and the bundle.
	 * @param registerHandler the handler that should be called
	 * @since 3.5.0
	 */
	void addBundleRegisterHandler(BiConsumer<String, SslBundle> registerHandler);

	/**
	 * Return the names of all bundles managed by this instance.
	 * @return the bundle names
	 * @since 3.4.0
	 */
	List<String> getBundleNames();

}
