/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.stream.Stream;

import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.core.convert.ConversionService;

/**
 * Context information for use by {@link BindHandler BindHandlers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public interface BindContext {

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
	 * Return a {@link Stream} of the {@link ConfigurationPropertySource sources} being
	 * used by the {@link Binder}.
	 * @return the sources
	 */
	Stream<ConfigurationPropertySource> streamSources();

	/**
	 * Return the {@link ConfigurationProperty} actually being bound or {@code null} if
	 * the property has not yet been determined.
	 * @return the configuration property (may be {@code null}).
	 */
	ConfigurationProperty getConfigurationProperty();

	/**
	 * Return the {@link PlaceholdersResolver} being used by the binder.
	 * @return the {@link PlaceholdersResolver} (never {@code null})
	 */
	PlaceholdersResolver getPlaceholdersResolver();

	/**
	 * Return the {@link ConversionService} used by the binder.
	 * @return the conversion service
	 */
	BinderConversionService getConversionService();

}
