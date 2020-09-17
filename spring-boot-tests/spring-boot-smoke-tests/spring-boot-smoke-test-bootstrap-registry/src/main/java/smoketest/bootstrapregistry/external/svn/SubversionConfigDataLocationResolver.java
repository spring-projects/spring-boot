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

package smoketest.bootstrapregistry.external.svn;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.boot.context.config.ConfigDataLocationResolver;
import org.springframework.boot.context.config.ConfigDataLocationResolverContext;

/**
 * {@link ConfigDataLocationResolver} for subversion.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLocationResolver implements ConfigDataLocationResolver<SubversionConfigDataLocation> {

	private static final String PREFIX = "svn:";

	@Override
	public boolean isResolvable(ConfigDataLocationResolverContext context, String location) {
		return location.startsWith(PREFIX);
	}

	@Override
	public List<SubversionConfigDataLocation> resolve(ConfigDataLocationResolverContext context, String location,
			boolean optional) throws ConfigDataLocationNotFoundException {
		String serverCertificate = context.getBinder().bind("spring.svn.server.certificate", String.class).orElse(null);
		return Collections.singletonList(
				new SubversionConfigDataLocation(location.substring(PREFIX.length()), serverCertificate));
	}

}
