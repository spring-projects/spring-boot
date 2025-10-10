/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.config;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.Binder;

/**
 * Context provided to {@link ConfigDataLocationResolver} methods.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.4.0
 */
public interface ConfigDataLocationResolverContext {

	/**
	 * Provides access to a binder that can be used to obtain previously contributed
	 * values.
	 * @return a binder instance
	 */
	Binder getBinder();

	/**
	 * Provides access to the parent {@link ConfigDataResource} that triggered the resolve
	 * or {@code null} if there is no available parent.
	 * @return the parent location
	 */
	@Nullable ConfigDataResource getParent();

	/**
	 * Provides access to the {@link ConfigurableBootstrapContext} shared across all
	 * {@link EnvironmentPostProcessor EnvironmentPostProcessors}.
	 * @return the bootstrap context
	 */
	ConfigurableBootstrapContext getBootstrapContext();

}
