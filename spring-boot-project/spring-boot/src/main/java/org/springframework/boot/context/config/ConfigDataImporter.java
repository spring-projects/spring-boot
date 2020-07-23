/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Imports {@link ConfigData} by {@link ConfigDataLocationResolver resolving} and
 * {@link ConfigDataLoader loading} imports. {@link ConfigDataLocation locations} are
 * tracked to ensure that they are not imported multiple times.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataImporter {

	private final ConfigDataLocationResolvers resolvers;

	private final ConfigDataLoaders loaders;

	private final Set<ConfigDataLocation> loadedLocations = new HashSet<>();

	/**
	 * Create a new {@link ConfigDataImporter} instance.
	 * @param resolvers the config data location resolvers
	 * @param loaders the config data loaders
	 */
	ConfigDataImporter(ConfigDataLocationResolvers resolvers, ConfigDataLoaders loaders) {
		this.resolvers = resolvers;
		this.loaders = loaders;
	}

	/**
	 * Resolve and load the given list of locations, filtering any that have been
	 * previously loaded.
	 * @param activationContext the activation context
	 * @param locationResolverContext the location resolver context
	 * @param locations the locations to resolve
	 * @return a map of the loaded locations and data
	 */
	Map<ConfigDataLocation, ConfigData> resolveAndLoad(ConfigDataActivationContext activationContext,
			ConfigDataLocationResolverContext locationResolverContext, List<String> locations) {
		try {
			Profiles profiles = (activationContext != null) ? activationContext.getProfiles() : null;
			return load(this.resolvers.resolveAll(locationResolverContext, locations, profiles));
		}
		catch (IOException ex) {
			throw new IllegalStateException("IO error on loading imports from " + locations, ex);
		}
	}

	private Map<ConfigDataLocation, ConfigData> load(List<ConfigDataLocation> locations) throws IOException {
		Map<ConfigDataLocation, ConfigData> result = new LinkedHashMap<>();
		for (int i = locations.size() - 1; i >= 0; i--) {
			ConfigDataLocation location = locations.get(i);
			if (this.loadedLocations.add(location)) {
				result.put(location, this.loaders.load(location));
			}
		}
		return Collections.unmodifiableMap(result);
	}

}
