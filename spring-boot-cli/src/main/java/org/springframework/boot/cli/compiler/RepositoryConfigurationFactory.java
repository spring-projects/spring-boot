/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.cli.compiler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * Factory used to create {@link RepositoryConfiguration}s.
 *
 * @author Andy Wilkinson
 * @author Dave Syer
 */
public final class RepositoryConfigurationFactory {

	private RepositoryConfigurationFactory() {
	}

	/**
	 * Create a new default repository configuration.
	 * @return the newly-created default repository configuration
	 */
	public static List<RepositoryConfiguration> createDefaultRepositoryConfiguration() {
		return convert(org.springframework.boot.aether.RepositoryConfigurationFactory
				.createDefaultRepositoryConfiguration());
	}

	private static List<RepositoryConfiguration> convert(
			List<org.springframework.boot.aether.RepositoryConfiguration> repositoryConfigurations) {
		List<RepositoryConfiguration> list = new ArrayList<RepositoryConfiguration>();
		for (org.springframework.boot.aether.RepositoryConfiguration repositoryConfiguration : repositoryConfigurations) {
			list.add(new RepositoryConfiguration(repositoryConfiguration.getName(),
					repositoryConfiguration.getUri(),
					repositoryConfiguration.getSnapshotsEnabled()));
		}
		return list;
	}

}
