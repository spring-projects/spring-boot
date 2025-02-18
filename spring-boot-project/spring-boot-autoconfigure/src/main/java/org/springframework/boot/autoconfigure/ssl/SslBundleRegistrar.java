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

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;

/**
 * Interface to be implemented by types that register {@link SslBundle} instances with an
 * {@link SslBundleRegistry}.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@FunctionalInterface
public interface SslBundleRegistrar {

	/**
	 * Callback method for registering {@link SslBundle}s with an
	 * {@link SslBundleRegistry}.
	 * @param registry the registry that accepts {@code SslBundle}s
	 */
	void registerBundles(SslBundleRegistry registry);

}
