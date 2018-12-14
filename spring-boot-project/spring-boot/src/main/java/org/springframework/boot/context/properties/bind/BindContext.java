/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

/**
 * Context information for use by {@link BindHandler BindHandlers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public interface BindContext {

	/**
	 * Return the source binder that is performing the bind operation.
	 * @return the source binder
	 */
	Binder getBinder();

	/**
	 * Return the current depth of the binding. Root binding starts with a depth of
	 * {@code 0}. Each subsequent property binding increases the depth by {@code 1}.
	 * @return the depth of the current binding
	 */
	int getDepth();

	/**
	 * Return an {@link Iterable} of the {@link ConfigurationPropertySource sources} being
	 * used by the {@link Binder}.
	 * @return the sources
	 */
	Iterable<ConfigurationPropertySource> getSources();

	/**
	 * Return the {@link ConfigurationProperty} actually being bound or {@code null} if
	 * the property has not yet been determined.
	 * @return the configuration property (may be {@code null}).
	 */
	ConfigurationProperty getConfigurationProperty();

}
