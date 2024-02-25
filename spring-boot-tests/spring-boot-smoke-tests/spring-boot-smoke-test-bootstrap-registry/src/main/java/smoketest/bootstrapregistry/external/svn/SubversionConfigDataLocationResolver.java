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

package smoketest.bootstrapregistry.external.svn;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.config.ConfigDataLocation;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;

/**
 * {@link ConfigDataLocationResolver} for subversion.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLocationResolver implements ConfigDataLocationResolver<SubversionConfigDataResource> {

	private static final String PREFIX = "svn:";

	/**
     * Determines if the given ConfigDataLocation is resolvable by this resolver.
     * 
     * @param context the ConfigDataLocationResolverContext
     * @param location the ConfigDataLocation to be resolved
     * @return true if the location has the specified prefix, false otherwise
     */
    @Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
		return location.hasPrefix(PREFIX);
	}

	/**
     * Resolves the Subversion configuration data resource for the given location.
     * 
     * @param context The context for resolving the configuration data location.
     * @param location The location of the configuration data.
     * @return A list containing the resolved Subversion configuration data resource.
     * @throws ConfigDataLocationNotFoundException If the configuration data location is not found.
     * @throws ConfigDataResourceNotFoundException If the configuration data resource is not found.
     */
    @Override
	public List<SubversionConfigDataResource> resolve(ConfigDataLocationResolverContext context,
			ConfigDataLocation location)
			throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
		String serverCertificate = context.getBinder().bind("spring.svn.server.certificate", String.class).orElse(null);
		return Collections
			.singletonList(new SubversionConfigDataResource(location.getNonPrefixedValue(PREFIX), serverCertificate));
	}

}
