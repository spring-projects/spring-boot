/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.compiler.grape;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.GroovyClassLoader;

import org.springframework.boot.aether.AetherEngine;

/**
 * Utility class to create a pre-configured {@link AetherGrapeEngine}.
 *
 * @author Andy Wilkinson
 */
public abstract class AetherGrapeEngineFactory {

	public static AetherGrapeEngine create(GroovyClassLoader classLoader,
			List<RepositoryConfiguration> repositoryConfigurations,
			DependencyResolutionContext dependencyManagement) {
		AetherEngine engine = AetherEngine.create(convert(repositoryConfigurations),
				dependencyManagement);
		return new AetherGrapeEngine(classLoader, engine, dependencyManagement);
	}

	private static List<org.springframework.boot.aether.RepositoryConfiguration> convert(
			List<RepositoryConfiguration> repositoryConfigurations) {
		List<org.springframework.boot.aether.RepositoryConfiguration> list = new ArrayList<org.springframework.boot.aether.RepositoryConfiguration>();
		for (RepositoryConfiguration repositoryConfiguration : repositoryConfigurations) {
			list.add(new org.springframework.boot.aether.RepositoryConfiguration(
					repositoryConfiguration.getName(), repositoryConfiguration.getUri(),
					repositoryConfiguration.getSnapshotsEnabled()));
		}
		return list;
	}

}
